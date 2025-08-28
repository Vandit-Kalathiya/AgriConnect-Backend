package com.agriconnect.Main.Backend.Service.Auth;

import com.agriconnect.Main.Backend.DTO.User.FarmerRegisterRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtResponse;
import com.agriconnect.Main.Backend.Entity.Session.Session;
import com.agriconnect.Main.Backend.Entity.Token.JwtToken;
import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Repository.Session.SessionRepository;
import com.agriconnect.Main.Backend.Repository.User.UserRepository;
import com.agriconnect.Main.Backend.Service.Token.TokenService;
import com.agriconnect.Main.Backend.Service.Twilio.TwilioOtpService;
import com.agriconnect.Main.Backend.jwt.JwtAuthenticationHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    AuthenticationManager manager;

    @Autowired
    JwtAuthenticationHelper jwtHelper;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TwilioOtpService twilioOtpService;

    private static final String HEX_CHARS = "0123456789abcdef";
    private static final SecureRandom random = new SecureRandom();

    public User registerUser(FarmerRegisterRequest farmerRegisterRequest) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = bCryptPasswordEncoder.encode(farmerRegisterRequest.getUsername()+farmerRegisterRequest.getPhoneNumber());

        if(userRepository.getUserByPhoneNumber(farmerRegisterRequest.getPhoneNumber()).isPresent()){
            throw new RuntimeException("Phone number already registered!");
        }

        User farmer = User.builder()
                .username(farmerRegisterRequest.getUsername())
                .phoneNumber(farmerRegisterRequest.getPhoneNumber())
                .uniqueHexAddress(generateRandomAddress())
                .address(farmerRegisterRequest.getAddress())
                .password(encodedPassword)
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .emailVerified(true)
                .build();

        return userRepository.save(farmer);
    }

    public static String generateRandomAddress() {
        StringBuilder address = new StringBuilder("0x");
        for (int i = 0; i < 40; i++) {
            address.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return address.toString();
    }

    public JwtResponse login(JwtRequest jwtRequest, HttpServletResponse response) {

        this.doAuthenticate(jwtRequest.getPhoneNumber());

        UserDetails userDetails = userDetailsService.loadUserByUsername(jwtRequest.getPhoneNumber());
        String token = jwtHelper.generateToken(jwtRequest.getPhoneNumber());

        JwtToken jwtToken = new JwtToken();
        jwtToken.setToken(token);
        jwtToken.setUsername(userDetails.getUsername());
        JwtToken savedToken = tokenService.saveToken(jwtToken);

        String sessionId = UUID.randomUUID().toString();
        this.saveSessionId(userDetails.getUsername(), sessionId);

        Cookie jwtCookie = new Cookie("jwt_token", token);
        jwtCookie.setHttpOnly(false);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(jwtCookie);

        JwtResponse jwtResponse = JwtResponse.builder().jwtToken(token).role("USER").build();
        return jwtResponse;
    }

    private void doAuthenticate(String phoneNumber) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(phoneNumber,null,userDetails.getAuthorities());
        try {
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }catch (BadCredentialsException e) {
            throw new BadCredentialsException("Mobile Number is not registered.");
        }
    }

    // Fetch current logged in user
    public ResponseEntity<?> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User) {
            return ResponseEntity.ok((User) principal);
        }

        throw new IllegalStateException("Authenticated principal is not a User instance.");
    }

    public String getSessionIdFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION_ID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public boolean isValidSessionId(String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionId(sessionId);

        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            // Ensure the session is not expired
            return LocalDateTime.now().isBefore(session.getExpiresAt());
        }

        return false;
    }


    public void saveSessionId(String username, String sessionId) {
        // Save session ID to the database for the user
        Session session = new Session();
        session.setUsername(username);
        session.setSessionId(sessionId);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // Example: 7-day expiration
        sessionRepository.save(session);
    }

    public void sendRegisterOtp(String mobileNumber) {
//        Optional<User> user = userRepository.getUserByPhoneNumber(mobileNumber);
//        if (!user.isPresent()) {
//            throw new RuntimeException("User not found with mobile number " + mobileNumber);
//        }
        twilioOtpService.sendOtp(mobileNumber);
    }

    public void sendLoginOtp(String mobileNumber) {
        Optional<User> user = userRepository.getUserByPhoneNumber(mobileNumber);
        if (!user.isPresent()) {
            throw new RuntimeException("User not found with mobile number " + mobileNumber);
        }
        twilioOtpService.sendOtp(mobileNumber);
    }

    public JwtResponse verifyAndLogin(String mobileNumber, String otp, HttpServletResponse response) {
        if (twilioOtpService.verifyOtp(mobileNumber, otp)) {
//            twilioOtpService.clearOtp(mobileNumber); // Clear OTP after use
            return this.login(JwtRequest.builder().phoneNumber(mobileNumber).build(), response);
        }
        throw new RuntimeException("Invalid OTP");
    }
}

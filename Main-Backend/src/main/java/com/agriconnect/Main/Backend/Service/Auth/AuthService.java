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
import com.agriconnect.Main.Backend.exception.BadRequestException;
import com.agriconnect.Main.Backend.exception.ConflictException;
import com.agriconnect.Main.Backend.exception.ResourceNotFoundException;
import com.agriconnect.Main.Backend.exception.UnauthorizedException;
import com.agriconnect.Main.Backend.jwt.JwtAuthenticationHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final SecureRandom random = new SecureRandom();
    private static final int SESSION_EXPIRY_DAYS = 7;

    private final AuthenticationManager manager;
    private final JwtAuthenticationHelper jwtHelper;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TwilioOtpService twilioOtpService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:604800000}")
    private Long jwtExpiration;

    @Autowired
    public AuthService(AuthenticationManager manager,
                       JwtAuthenticationHelper jwtHelper,
                       UserDetailsService userDetailsService,
                       TokenService tokenService,
                       SessionRepository sessionRepository,
                       UserRepository userRepository,
                       TwilioOtpService twilioOtpService,
                       BCryptPasswordEncoder passwordEncoder) {
        this.manager = manager;
        this.jwtHelper = jwtHelper;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.twilioOtpService = twilioOtpService;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(FarmerRegisterRequest farmerRegisterRequest) {
        logger.info("Attempting to register user with phone number: {}", farmerRegisterRequest.getPhoneNumber());

        if (farmerRegisterRequest.getPhoneNumber() == null || farmerRegisterRequest.getPhoneNumber().isEmpty()) {
            logger.error("Registration failed: Phone number is null or empty");
            throw new BadRequestException("Phone number cannot be empty");
        }

        if (userRepository.getUserByPhoneNumber(farmerRegisterRequest.getPhoneNumber()).isPresent()) {
            logger.error("Registration failed: Phone number already exists - {}", farmerRegisterRequest.getPhoneNumber());
            throw new ConflictException("Phone number already registered!");
        }

        String encodedPassword = passwordEncoder.encode(
            farmerRegisterRequest.getUsername() + farmerRegisterRequest.getPhoneNumber()
        );

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

        User savedUser = userRepository.save(farmer);
        logger.info("User registered successfully with phone number: {}", savedUser.getPhoneNumber());
        return savedUser;
    }

    public static String generateRandomAddress() {
        StringBuilder address = new StringBuilder("0x");
        for (int i = 0; i < 40; i++) {
            address.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return address.toString();
    }

    public JwtResponse login(JwtRequest jwtRequest, HttpServletResponse response) {
        logger.info("Login attempt for phone number: {}", jwtRequest.getPhoneNumber());

        try {
            this.doAuthenticate(jwtRequest.getPhoneNumber());

            UserDetails userDetails = userDetailsService.loadUserByUsername(jwtRequest.getPhoneNumber());
            String token = jwtHelper.generateToken(jwtRequest.getPhoneNumber());

            JwtToken jwtToken = new JwtToken();
            jwtToken.setToken(token);
            jwtToken.setUsername(userDetails.getUsername());
            tokenService.saveToken(jwtToken);

            String sessionId = UUID.randomUUID().toString();
            this.saveSessionId(userDetails.getUsername(), sessionId);

            Cookie jwtCookie = new Cookie("jwt_token", token);
            jwtCookie.setHttpOnly(true); // Changed to true for security
            jwtCookie.setSecure(false); // Set to true in production with HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge((int) (jwtExpiration / 1000)); // Convert to seconds
            response.addCookie(jwtCookie);

            logger.info("User logged in successfully: {}", jwtRequest.getPhoneNumber());
            return JwtResponse.builder()
                    .jwtToken(token)
                    .role("USER")
                    .build();

        } catch (BadCredentialsException e) {
            logger.error("Login failed for phone number: {} - Invalid credentials", jwtRequest.getPhoneNumber());
            throw new UnauthorizedException("Invalid phone number or credentials");
        }
    }

    private void doAuthenticate(String phoneNumber) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(phoneNumber, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception e) {
            logger.error("Authentication failed for phone number: {}", phoneNumber);
            throw new BadCredentialsException("Mobile Number is not registered.");
        }
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String phoneNumber = ((UserDetails) principal).getUsername();
            return userRepository.getUserByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "phoneNumber", phoneNumber));
        }

        logger.error("Authenticated principal is not a UserDetails instance");
        throw new UnauthorizedException("Invalid authentication principal");
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
        logger.debug("Saving session for user: {}", username);
        Session session = new Session();
        session.setUsername(username);
        session.setSessionId(sessionId);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(SESSION_EXPIRY_DAYS));
        sessionRepository.save(session);
        logger.debug("Session saved successfully for user: {}", username);
    }

    public void sendRegisterOtp(String mobileNumber) {
        logger.info("Sending registration OTP to: {}", mobileNumber);
        
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new BadRequestException("Mobile number cannot be empty");
        }
        
        try {
            twilioOtpService.sendOtp(mobileNumber);
            logger.info("Registration OTP sent successfully to: {}", mobileNumber);
        } catch (Exception e) {
            logger.error("Failed to send registration OTP to: {}", mobileNumber, e);
            throw new BadRequestException("Failed to send OTP. Please try again.");
        }
    }

    public void sendLoginOtp(String mobileNumber) {
        logger.info("Sending login OTP to: {}", mobileNumber);
        
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new BadRequestException("Mobile number cannot be empty");
        }
        
        Optional<User> user = userRepository.getUserByPhoneNumber(mobileNumber);
        if (!user.isPresent()) {
            logger.error("Login OTP request for unregistered number: {}", mobileNumber);
            throw new ResourceNotFoundException("User", "mobileNumber", mobileNumber);
        }
        
        try {
            twilioOtpService.sendOtp(mobileNumber);
            logger.info("Login OTP sent successfully to: {}", mobileNumber);
        } catch (Exception e) {
            logger.error("Failed to send login OTP to: {}", mobileNumber, e);
            throw new BadRequestException("Failed to send OTP. Please try again.");
        }
    }

    public JwtResponse verifyAndLogin(String mobileNumber, String otp, HttpServletResponse response) {
        logger.info("Verifying OTP for: {}", mobileNumber);
        
        if (mobileNumber == null || otp == null || otp.isEmpty()) {
            throw new BadRequestException("Mobile number and OTP are required");
        }
        
        if (twilioOtpService.verifyOtp(mobileNumber, otp)) {
            logger.info("OTP verified successfully for: {}", mobileNumber);
            return this.login(JwtRequest.builder().phoneNumber(mobileNumber).build(), response);
        }
        
        logger.error("Invalid OTP provided for: {}", mobileNumber);
        throw new UnauthorizedException("Invalid or expired OTP");
    }
}

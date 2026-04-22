package com.agriconnect.api.gateway.Service.Auth;

import com.agriconnect.api.gateway.DTO.User.FarmerRegisterRequest;
import com.agriconnect.api.gateway.DTO.Jwt.JwtRequest;
import com.agriconnect.api.gateway.DTO.Jwt.JwtResponse;
import com.agriconnect.api.gateway.Entity.Session.Session;
import com.agriconnect.api.gateway.Entity.Token.JwtToken;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Repository.Session.SessionRepository;
import com.agriconnect.api.gateway.Repository.User.UserRepository;
import com.agriconnect.api.gateway.Service.Cache.CacheService;
import com.agriconnect.api.gateway.Service.Token.TokenService;
import com.agriconnect.api.gateway.exception.BadRequestException;
import com.agriconnect.api.gateway.exception.ConflictException;
import com.agriconnect.api.gateway.exception.ResourceNotFoundException;
import com.agriconnect.api.gateway.exception.UnauthorizedException;
import com.agriconnect.api.gateway.jwt.JwtAuthenticationHelper;
import com.agriconnect.api.gateway.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final SecureRandom random = new SecureRandom();
    private static final int SESSION_EXPIRY_DAYS = 7;

    private static final String USER_PHONE_KEY_PREFIX = "user:phone:";
    private static final String USER_EMAIL_KEY_PREFIX = "user:email:";
    private static final Duration USER_TTL = Duration.ofHours(12);

    private final AuthenticationManager manager;
    private final JwtAuthenticationHelper jwtHelper;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationEventPublisher notificationEventPublisher;
    private final CacheService cacheService;

    @Value("${jwt.expiration:604800000}")
    private Long jwtExpiration;

    @Value("${notification.topics.auth}")
    private String authTopic;

    @Autowired
    public AuthService(AuthenticationManager manager,
            JwtAuthenticationHelper jwtHelper,
            UserDetailsService userDetailsService,
            TokenService tokenService,
            SessionRepository sessionRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            NotificationEventPublisher notificationEventPublisher,
            CacheService cacheService) {
        this.manager = manager;
        this.jwtHelper = jwtHelper;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationEventPublisher = notificationEventPublisher;
        this.cacheService = cacheService;
    }

    @Transactional
    public User registerUser(FarmerRegisterRequest farmerRegisterRequest) {
        logger.info("Attempting to register user with phone number: {}", farmerRegisterRequest.getPhoneNumber());

        if (farmerRegisterRequest.getPhoneNumber() == null || farmerRegisterRequest.getPhoneNumber().isEmpty()) {
            throw new BadRequestException("Phone number cannot be empty");
        }

        if (userRepository.getUserByPhoneNumber(farmerRegisterRequest.getPhoneNumber()).isPresent()) {
            throw new ConflictException("Phone number already registered!");
        }

        if (userRepository.findByEmail(farmerRegisterRequest.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered!");
        }

        String encodedPassword = passwordEncoder.encode(farmerRegisterRequest.getPassword());

        User farmer = User.builder()
                .username(farmerRegisterRequest.getUsername())
                .phoneNumber(farmerRegisterRequest.getPhoneNumber())
                .email(farmerRegisterRequest.getEmail())
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

    @Transactional
    public JwtResponse login(JwtRequest jwtRequest, HttpServletResponse response) {
        logger.info("Login attempt for username: {}", jwtRequest.getUsername());

        try {
            doAuthenticate(jwtRequest.getUsername(), jwtRequest.getPassword());

            UserDetails userDetails = userDetailsService.loadUserByUsername(jwtRequest.getUsername());
            String token = jwtHelper.generateToken(userDetails.getUsername());

            JwtToken jwtToken = new JwtToken();
            jwtToken.setToken(token);
            jwtToken.setUsername(userDetails.getUsername());
            jwtToken.setRevoked(false);
            jwtToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpiration / 1000));
            tokenService.saveToken(jwtToken);

            String sessionId = UUID.randomUUID().toString();
            saveSessionId(userDetails.getUsername(), sessionId);

            Cookie jwtCookie = new Cookie("jwt_token", token);
            jwtCookie.setHttpOnly(false);
            jwtCookie.setSecure(false);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge((int) (jwtExpiration / 1000));
            response.addCookie(jwtCookie);

            logger.info("User logged in successfully: {}", jwtRequest.getUsername());

            // Publish login notification (in-app only to avoid spam)
            try {
                User loggedInUser = getUserByUsernameOrEmail(jwtRequest.getUsername());
                if (loggedInUser != null) {
                    notificationEventPublisher.publish(authTopic,
                            notificationEventPublisher.buildEvent(
                                    "AUTH_NEW_DEVICE_LOGIN",
                                    loggedInUser.getId(),
                                    "auth.new.device.login",
                                    List.of("IN_APP"),
                                    Map.of(
                                            "loginAt", Instant.now().toString(),
                                            "userName",
                                            loggedInUser.getUsername() != null ? loggedInUser.getUsername() : "Farmer"),
                                    Priority.NORMAL,
                                    "login-" + loggedInUser.getId(),
                                    loggedInUser.getEmail(),
                                    loggedInUser.getPhoneNumber()));
                }
            } catch (Exception ex) {
                logger.warn("[NOTIFY] Failed to publish AUTH_NEW_DEVICE_LOGIN: {}", ex.getMessage());
            }

            return JwtResponse.builder()
                    .jwtToken(token)
                    .role("USER")
                    .build();

        } catch (BadCredentialsException e) {
            logger.error("Login failed for username: {} - Invalid credentials", jwtRequest.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    private void doAuthenticate(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = manager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            logger.error("Authentication failed for username: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private User getUserByUsernameOrEmail(String username) {
        String cacheKey;
        if (username.matches("^[0-9]{10}$")) {
            cacheKey = USER_PHONE_KEY_PREFIX + username;
            return cacheService.get(cacheKey, User.class).orElseGet(() -> {
                User user = userRepository.getUserByPhoneNumber(username).orElse(null);
                if (user != null) {
                    cacheService.save(cacheKey, user, USER_TTL);
                    logger.debug("Cached user by phone: {}", username);
                }
                return user;
            });
        } else {
            cacheKey = USER_EMAIL_KEY_PREFIX + username;
            return cacheService.get(cacheKey, User.class).orElseGet(() -> {
                User user = userRepository.findByEmail(username).orElse(null);
                if (user != null) {
                    cacheService.save(cacheKey, user, USER_TTL);
                    logger.debug("Cached user by email: {}", username);
                }
                return user;
            });
        }
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String phoneNumber = ((UserDetails) principal).getUsername();
            String cacheKey = USER_PHONE_KEY_PREFIX + phoneNumber;
            return cacheService.get(cacheKey, User.class).orElseGet(() -> {
                User user = userRepository.getUserByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "phoneNumber", phoneNumber));
                cacheService.save(cacheKey, user, USER_TTL);
                logger.debug("Cached current user: {}", phoneNumber);
                return user;
            });
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

    @Transactional(readOnly = true)
    public boolean isValidSessionId(String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            return LocalDateTime.now().isBefore(session.getExpiresAt());
        }
        return false;
    }

    @Transactional
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

    public static String generateRandomAddress() {
        StringBuilder address = new StringBuilder("0x");
        for (int i = 0; i < 40; i++) {
            address.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return address.toString();
    }
}

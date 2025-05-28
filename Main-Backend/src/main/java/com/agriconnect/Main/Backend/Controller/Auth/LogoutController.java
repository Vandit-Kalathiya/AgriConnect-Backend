package com.agriconnect.Main.Backend.Controller.Auth;

import com.agriconnect.Main.Backend.Repository.BlackListedTokenRepo;
import com.agriconnect.Main.Backend.Repository.Session.SessionRepository;
import com.agriconnect.Main.Backend.Service.BlackListedTokenService;
import com.agriconnect.Main.Backend.Service.Token.TokenService;
import com.agriconnect.Main.Backend.jwt.JwtAuthenticationHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/auth")
public class LogoutController {

    @Autowired
    private JwtAuthenticationHelper jwtHelper;

    @Autowired
    private BlackListedTokenService blacklistedTokenService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private BlackListedTokenRepo blackListedTokenRepo;

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String tokenHeader, HttpServletRequest request, HttpServletResponse response) {
        // Step 1: Check if the token is provided in the Authorization header
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid token format");
        }

        // Step 2: Extract the token from the Authorization header
        String token = tokenHeader.substring(7);

        // Step 3: Check if the JWT cookie exists
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("jwt_token")) {
                    // Step 4: Invalidate the JWT cookie by setting its max age to 0
                    cookie.setMaxAge(0);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(true); // Set to true for HTTPS
                    cookie.setPath("/"); // Ensure the cookie is deleted on all paths
                    response.addCookie(cookie);
                    break;
                }
            }
        }

        try {
            // Step 5: Invalidate the session (if needed)
            // If you are using sessions stored in a database or cache, clear them here
            sessionRepository.deleteAll();

            // Step 6: Delete the token from the token service (e.g., database or cache)
            tokenService.deleteToken(token); // Assuming tokenService handles token removal from a store

            // Step 7: Optionally blacklist the token if your system maintains blacklisted tokens
            Date expiryDate = jwtHelper.getExpirationDateFromToken(token);
            blacklistedTokenService.blacklistToken(token, expiryDate);

            // Step 8: Return success response
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            // Step 9: Handle any errors during the logout process
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred during logout: " + e.getMessage());
        }
    }
}

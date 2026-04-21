package com.agriconnect.api.gateway.security;

import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Repository.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user;

        if (username.matches("^[0-9]{10}$")) {
            user = userRepository.getUserByPhoneNumber(username);
        } else {
            user = userRepository.findByEmail(username);
        }

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email or phone number: " + username);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.get().getPhoneNumber())
                .password(user.get().getPassword())
                .authorities("USER")
                .build();
    }
}

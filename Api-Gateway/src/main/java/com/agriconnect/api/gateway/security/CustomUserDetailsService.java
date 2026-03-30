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
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        Optional<User> user = userRepository.getUserByPhoneNumber(phoneNumber);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Mobile number not registered: " + phoneNumber);
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.get().getPhoneNumber())
                .password(user.get().getPassword())
                .authorities("USER")
                .build();
    }
}

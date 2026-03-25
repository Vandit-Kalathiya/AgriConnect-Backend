package com.agriconnect.Main.Backend.security;


import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Repository.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
            throw new UsernameNotFoundException("Mobile Number not registered: " + phoneNumber);
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.get().getPhoneNumber())
                .password("")
                .authorities("USER")
                .build();
    }
}

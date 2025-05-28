package com.agriconnect.Main.Backend.security;


import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Repository.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        Optional<User> user = userRepository.getUserByPhoneNumber(phoneNumber);
        if (!user.isPresent()) {
            throw new UsernameNotFoundException("Mobile Number not registered: " + phoneNumber);
        }
        // Return UserDetails (no password since it's OTP-based)
        return org.springframework.security.core.userdetails.User
                .withUsername(user.get().getPhoneNumber())
                .password("") // No password for OTP flow
                .authorities("USER") // Adjust roles as needed
                .build();
    }

//	private Collection<? extends GrantedAuthority> getAuthorities(Set<Role> roles) {
//		return roles.stream()
//				.map(role -> new SimpleGrantedAuthority(role.getRoleName()))  // Assuming role.getRoleName() returns a String
//				.collect(Collectors.toSet());
//	}
}

package com.pg.security;

import com.pg.entity.User;
import com.pg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Try to find by email
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> {
                    // 2. Fallback to username for legacy users or internal lookups
                    return userRepository.findByUsername(identifier)
                            .orElseThrow(() -> new UsernameNotFoundException(
                                    "User not found with identifier: " + identifier));
                });

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // Use email as the subject for JWT tokens
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountLocked(user.getFailedLoginAttempts() >= 5)
                .disabled(user.getStatus().name().equals("INACTIVE"))
                .build();
    }
}

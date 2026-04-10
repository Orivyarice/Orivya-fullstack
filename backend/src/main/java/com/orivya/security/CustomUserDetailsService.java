package com.orivya.security;

import com.orivya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * CustomUserDetailsService — Loads user from the database by email.
 *
 * WHY this is a separate class (not inside SecurityConfig):
 * ─────────────────────────────────────────────────────────
 * If UserDetailsService is a @Bean inside SecurityConfig,
 * it creates a CIRCULAR DEPENDENCY:
 *
 *   SecurityConfig → needs JwtAuthFilter
 *   JwtAuthFilter  → needs UserDetailsService
 *   UserDetailsService → is inside SecurityConfig (not created yet!)
 *                                    ↑___________ LOOP = App crash
 *
 * By moving it here as a separate @Service, the cycle is broken:
 *
 *   SecurityConfig → needs JwtAuthFilter ✅
 *   JwtAuthFilter  → needs UserDetailsService ✅ (separate bean, always available)
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their email address.
     * Called by Spring Security during login and JWT validation.
     *
     * @param email the user's email (used as username in this app)
     * @return UserDetails object with email, hashed password, and role
     * @throws UsernameNotFoundException if no user found with that email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        // Convert Role enum (ADMIN/CUSTOMER) to Spring Security authority
                        // Spring Security requires "ROLE_" prefix for hasRole() checks
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                        )
                ))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email)
                );
    }
}

package com.orivya.service;

import com.orivya.dto.*;
import com.orivya.entity.User;
import com.orivya.repository.UserRepository;
import com.orivya.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthService — Handles user registration and login.
 *
 * Registration flow:
 *   1. Check email doesn't already exist
 *   2. Hash the password with BCrypt
 *   3. Save user to DB
 *   4. Generate JWT token
 *   5. Return token + user info
 *
 * Login flow:
 *   1. Authenticate email + password via AuthenticationManager
 *   2. If valid → generate JWT token
 *   3. Return token + user info
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new customer account.
     */
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // 2. Build user entity — hash password with BCrypt
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // HASHED
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(User.Role.CUSTOMER) // All registrations are CUSTOMER
                .build();

        // 3. Save to DB
        User savedUser = userRepository.save(user);

        // 4. Generate JWT token
        String token = generateTokenForUser(savedUser);

        // 5. Return response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .message("Registration successful! Welcome to Orivya Rice.")
                .build();
    }

    /**
     * Register admin account (called only once, or via a protected endpoint).
     */
    public AuthResponse registerAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered.");
        }

        User admin = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.ADMIN) // ADMIN role
                .build();

        User savedAdmin = userRepository.save(admin);
        String token = generateTokenForUser(savedAdmin);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedAdmin.getId())
                .name(savedAdmin.getName())
                .email(savedAdmin.getEmail())
                .role(savedAdmin.getRole().name())
                .message("Admin account created successfully.")
                .build();
    }

    /**
     * Login with email and password.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate — this checks email + password via Spring Security
        //    Throws AuthenticationException if credentials are wrong
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        // 2. If we reach here, credentials are valid — load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate JWT token
        String token = generateTokenForUser(user);

        // 4. Return response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Login successful!")
                .build();
    }

    /**
     * Helper: Create a JWT token for a user, including role in claims.
     */
    private String generateTokenForUser(User user) {
        // Build Spring Security UserDetails object
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            )
        );

        // Add extra claims (role, userId) into the token
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("userId", user.getId());
        extraClaims.put("name", user.getName());

        return jwtUtil.generateToken(extraClaims, userDetails);
    }
}

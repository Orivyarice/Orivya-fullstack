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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtUtil            jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OtpService         otpService; // NEW

    // ── REGISTRATION (Step 1 of 2) ────────────────────────────────
    /**
     * Step 1 — Validate data, save UNVERIFIED user, send OTP.
     * User is saved with isVerified=false.
     * They cannot login until email is verified.
     */
    public AuthResponse register(RegisterRequest request) {
        // 1. Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered. Please login.");
        }

        // 2. Build full address string from parts
        String fullAddress = buildAddress(request);

        // 3. Save user as UNVERIFIED
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(fullAddress)
                .pincode(request.getPincode())
                .street(request.getStreet())
                .village(request.getVillage())
                .city(request.getCity())
                .state(request.getState())
                .role(User.Role.CUSTOMER)
                .isVerified(false) // NOT verified yet
                .build();

        userRepository.save(user);

        // 4. Send OTP to email
        otpService.sendRegistrationOtp(request.getEmail(), request.getName());

        return AuthResponse.builder()
                .email(request.getEmail())
                .name(request.getName())
                .message("OTP_SENT") // Frontend checks this to show OTP screen
                .build();
    }

    // ── REGISTRATION OTP VERIFY (Step 2 of 2) ────────────────────
    /**
     * Step 2 — User enters OTP. If correct, mark as VERIFIED.
     * Now they can login.
     */
    public AuthResponse verifyRegistrationOtp(String email, String otp) {
        String result = otpService.verifyRegistrationOtp(email, otp);

        if ("EXPIRED".equals(result)) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
        if ("MAX_ATTEMPTS".equals(result)) {
            throw new RuntimeException("Too many wrong attempts. Please request a new OTP.");
        }
        if ("INVALID".equals(result)) {
            throw new RuntimeException("Invalid OTP. Please check and try again.");
        }

        // OTP is VALID — mark user as verified
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        user.setIsVerified(true);
        userRepository.save(user);

        return AuthResponse.builder()
                .email(email)
                .name(user.getName())
                .role(user.getRole().name())
                .message("VERIFIED")
                .build();
    }

    // ── LOGIN (Step 1 of 2) ───────────────────────────────────────
    /**
     * Step 1 — Verify email + password.
     * If correct, send OTP to email.
     * Return "OTP_SENT" — frontend shows OTP screen.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate email + password
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password.");
        }

        // 2. Load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found."));

        // 3. Check if email is verified
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            // Resend registration OTP so they can verify
            otpService.sendRegistrationOtp(user.getEmail(), user.getName());
            throw new RuntimeException("Email not verified. A new OTP has been sent to " + user.getEmail());
        }

        // 4. Send login OTP
        otpService.sendLoginOtp(user.getEmail(), user.getName());

        return AuthResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .message("OTP_SENT")
                .build();
    }

    // ── LOGIN OTP VERIFY (Step 2 of 2) ───────────────────────────
    /**
     * Step 2 — User enters login OTP.
     * If correct, generate JWT and return it.
     */
    public AuthResponse verifyLoginOtp(String email, String otp) {
        String result = otpService.verifyLoginOtp(email, otp);

        if ("EXPIRED".equals(result)) {
            throw new RuntimeException("OTP has expired. Please login again.");
        }
        if ("MAX_ATTEMPTS".equals(result)) {
            throw new RuntimeException("Too many wrong attempts. Please login again.");
        }
        if ("INVALID".equals(result)) {
            throw new RuntimeException("Invalid OTP. Please check and try again.");
        }

        // OTP valid — generate JWT token
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        String token = generateTokenForUser(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("LOGIN_SUCCESS")
                .build();
    }

    // ── RESEND OTP ────────────────────────────────────────────────
    public AuthResponse resendOtp(String email, String type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not registered."));

        if ("REGISTRATION".equals(type)) {
            otpService.resendRegistrationOtp(email, user.getName());
        } else if ("LOGIN".equals(type)) {
            otpService.resendLoginOtp(email, user.getName());
        }

        return AuthResponse.builder()
                .email(email)
                .message("OTP_RESENT")
                .build();
    }

    // ── ADMIN REGISTER (unchanged) ────────────────────────────────
    public AuthResponse registerAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered.");
        }
        User admin = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.ADMIN)
                .isVerified(true) // Admin is auto-verified
                .build();
        User saved = userRepository.save(admin);
        String token = generateTokenForUser(saved);
        return AuthResponse.builder()
                .token(token).tokenType("Bearer")
                .userId(saved.getId()).name(saved.getName())
                .email(saved.getEmail()).role(saved.getRole().name())
                .message("Admin account created successfully.")
                .build();
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private String buildAddress(RegisterRequest r) {
        StringBuilder sb = new StringBuilder();
        if (r.getStreet()  != null && !r.getStreet().isEmpty())  sb.append(r.getStreet()).append(", ");
        if (r.getVillage() != null && !r.getVillage().isEmpty()) sb.append(r.getVillage()).append(", ");
        if (r.getCity()    != null && !r.getCity().isEmpty())    sb.append(r.getCity()).append(", ");
        if (r.getState()   != null && !r.getState().isEmpty())   sb.append(r.getState()).append(" ");
        if (r.getPincode() != null && !r.getPincode().isEmpty()) sb.append("- ").append(r.getPincode());
        return sb.toString().trim();
    }

    private String generateTokenForUser(User user) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role",   user.getRole().name());
        extraClaims.put("userId", user.getId());
        extraClaims.put("name",   user.getName());
        return jwtUtil.generateToken(extraClaims, userDetails);
    }
}
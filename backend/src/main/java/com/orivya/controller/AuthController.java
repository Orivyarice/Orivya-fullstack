package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.dto.AuthResponse;
import com.orivya.dto.LoginRequest;
import com.orivya.dto.RegisterRequest;
import com.orivya.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * AuthController — All authentication endpoints.
 *
 * REGISTRATION FLOW (2 steps):
 *   POST /api/auth/register              → Save unverified user, send OTP
 *   POST /api/auth/verify-registration   → Verify OTP, mark user verified
 *
 * LOGIN FLOW (2 steps):
 *   POST /api/auth/login                 → Check password, send OTP
 *   POST /api/auth/verify-login          → Verify OTP, return JWT token
 *
 * RESEND:
 *   POST /api/auth/resend-otp            → Resend OTP (registration or login)
 *
 * ADMIN:
 *   POST /api/auth/register-admin        → Create admin (one-time use)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    // ── STEP 1: REGISTER ─────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse res = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(true)
                            .message("OTP sent to " + request.getEmail())
                            .data(res).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ── STEP 2: VERIFY REGISTRATION OTP ──────────────────────────
    // Body: { "email": "user@gmail.com", "otp": "123456" }
    @PostMapping("/verify-registration")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistration(
            @RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String otp   = body.get("otp");
            if (email == null || otp == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<AuthResponse>builder()
                                .success(false).message("Email and OTP are required.").build());
            }
            AuthResponse res = authService.verifyRegistrationOtp(email, otp);
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true).message("Email verified! Please login.")
                    .data(res).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ── STEP 1: LOGIN ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse res = authService.login(request);
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true).message("OTP sent to your email.")
                    .data(res).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ── STEP 2: VERIFY LOGIN OTP ──────────────────────────────────
    // Body: { "email": "user@gmail.com", "otp": "123456" }
    @PostMapping("/verify-login")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLogin(
            @RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String otp   = body.get("otp");
            if (email == null || otp == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<AuthResponse>builder()
                                .success(false).message("Email and OTP are required.").build());
            }
            AuthResponse res = authService.verifyLoginOtp(email, otp);
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true).message("Login successful!")
                    .data(res).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ── RESEND OTP ────────────────────────────────────────────────
    // Body: { "email": "user@gmail.com", "type": "REGISTRATION" | "LOGIN" }
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> resendOtp(
            @RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String type  = body.get("type");
            AuthResponse res = authService.resendOtp(email, type);
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true).message("New OTP sent to " + email)
                    .data(res).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ── ADMIN REGISTER ────────────────────────────────────────────
    @PostMapping("/register-admin")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAdmin(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse res = authService.registerAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(true).message("Admin account created!")
                            .data(res).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }
}
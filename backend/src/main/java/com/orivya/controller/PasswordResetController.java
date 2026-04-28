package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * PasswordResetController — Forgot password endpoints.
 *
 * NEW FILE — does not modify AuthController or any existing endpoint.
 *
 * Endpoints added:
 *   POST /api/auth/forgot-password  → send reset OTP to email
 *   POST /api/auth/reset-password   → verify OTP + set new password
 *
 * Security:
 *   - Both endpoints are PUBLIC (no JWT needed — user forgot password)
 *   - Configured in SecurityConfig via existing permitAll() for /api/auth/**
 *   - Existing /api/auth/** permitAll rule already covers these endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/forgot-password
     * Body: { "email": "user@gmail.com" }
     *
     * - Checks if email exists
     * - If yes: generates OTP, saves to otp_verification table, sends via SendGrid
     * - Always returns success (prevents email enumeration)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Email is required.")
                            .build());
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Please enter a valid email address.")
                            .build());
        }

        try {
            // Always succeeds (silently ignores unregistered emails)
            passwordResetService.forgotPassword(email.trim().toLowerCase());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("If this email is registered, an OTP has been sent.")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to send OTP: " + e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/auth/reset-password
     * Body: { "email": "...", "otp": "123456", "newPassword": "NewPass1" }
     *
     * - Verifies OTP (checks expiry, attempts, correctness)
     * - BCrypt-hashes new password
     * - Updates password in users table
     * - Deletes OTP from otp_verification table
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody Map<String, String> body) {

        String email       = body.get("email");
        String otp         = body.get("otp");
        String newPassword = body.get("newPassword");

        // Input validation
        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Email, OTP, and new password are required.")
                            .build());
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Password must be at least 8 characters.")
                            .build());
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Password must contain at least one uppercase letter.")
                            .build());
        }
        if (!newPassword.matches(".*[0-9].*")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Password must contain at least one number.")
                            .build());
        }

        try {
            String result = passwordResetService.resetPassword(
                    email.trim().toLowerCase(), otp.trim(), newPassword);

            return switch (result) {
                case "SUCCESS" -> ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password reset successful! Please login with your new password.")
                        .build());

                case "EXPIRED" -> ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("OTP has expired. Please request a new one.")
                                .build());

                case "MAX_ATTEMPTS" -> ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("Too many wrong attempts. Please request a new OTP.")
                                .build());

                case "USER_NOT_FOUND" -> ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("Email not registered.")
                                .build());

                default -> ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("Invalid OTP. Please check and try again.")
                                .build());
            };

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Reset failed: " + e.getMessage())
                            .build());
        }
    }
}
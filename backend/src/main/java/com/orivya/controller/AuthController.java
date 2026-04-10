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

/**
 * AuthController — Handles user registration and login.
 *
 * Public Endpoints (no JWT needed):
 *  POST /api/auth/register  → Register as customer
 *  POST /api/auth/login     → Login and get JWT token
 *  POST /api/auth/register-admin → Create admin (protect in production!)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow requests from any frontend origin
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new customer account.
     *
     * Request Body:
     * {
     *   "name": "Ravi Kumar",
     *   "email": "ravi@gmail.com",
     *   "password": "ravi1234",
     *   "phone": "9876543210"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(true)
                            .message("Registration successful!")
                            .data(response)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/auth/login
     * Login with email and password. Returns JWT token.
     *
     * Request Body:
     * {
     *   "email": "ravi@gmail.com",
     *   "password": "ravi1234"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "token": "eyJhbGci...",
     *     "tokenType": "Bearer",
     *     "role": "CUSTOMER"
     *   }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true)
                    .message("Login successful!")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false)
                            .message("Invalid email or password.")
                            .build());
        }
    }

    /**
     * POST /api/auth/register-admin
     * Create an admin account.
     * NOTE: Protect this endpoint in production or remove after first use.
     */
    @PostMapping("/register-admin")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAdmin(
            @Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.registerAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(true)
                            .message("Admin account created!")
                            .data(response)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}

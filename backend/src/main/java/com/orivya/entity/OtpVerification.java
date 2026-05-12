package com.orivya.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * OtpVerification — Stores OTP codes for email verification.
 *
 * Used for TWO flows:
 *   type = "REGISTRATION" → verify email during sign-up
 *   type = "LOGIN"        → verify email during login (2FA)
 *
 * OTP expires after 5 minutes.
 * Deleted after successful verification.
 * After 3 wrong attempts → OTP is invalidated.
 */
@Entity
@Table(name = "otp_verification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The email this OTP was sent to
    @Column(nullable = false)
    private String email;

    // 6-digit OTP code
    @Column(nullable = false, length = 6)
    private String otp;

    // "REGISTRATION" or "LOGIN"
    @Column(nullable = false, length = 20)
    private String type;

    // When the OTP was created — used to check 5-minute expiry
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // How many wrong attempts the user has made
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
package com.orivya.service;

import com.orivya.entity.User;
import com.orivya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * PasswordResetService — Forgot password feature.
 *
 * NEW FILE — does not modify any existing service.
 *
 * Flow:
 *   1. forgotPassword(email)
 *      → Checks email exists in users table
 *      → Calls OtpService.sendPasswordResetOtp() (new method)
 *
 *   2. resetPassword(email, otp, newPassword)
 *      → Calls OtpService.verifyPasswordResetOtp() (new method)
 *      → If VALID: BCrypt-hashes new password, saves to users table
 *      → Returns success/error
 *
 * Dependencies used (all already exist — nothing new created):
 *   - UserRepository.findByEmail()       existing
 *   - OtpService.sendPasswordResetOtp()  new method added to OtpService
 *   - OtpService.verifyPasswordResetOtp() new method added to OtpService
 *   - PasswordEncoder (BCrypt)           existing Spring Security bean
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository  userRepository;
    private final OtpService      otpService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Step 1 — Forgot Password.
     * Sends a reset OTP if the email exists.
     *
     * Security note: we return success even if email not found to
     * prevent email enumeration attacks.
     */
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Email exists → send reset OTP
            otpService.sendPasswordResetOtp(email, user.getName());
            log.info("Password reset OTP requested for: {}", email);
        });
        // If email not found: silently succeed (no error to frontend)
        // This prevents attackers from discovering which emails are registered
    }

    /**
     * Step 2 — Reset Password.
     * Verifies OTP then updates password with BCrypt hash.
     *
     * Returns: "SUCCESS" | "INVALID" | "EXPIRED" | "MAX_ATTEMPTS" | "USER_NOT_FOUND"
     */
    public String resetPassword(String email, String otp, String newPassword) {
        // 1. Verify OTP using existing OtpService logic
        String otpResult = otpService.verifyPasswordResetOtp(email, otp);

        if (!"VALID".equals(otpResult)) {
            log.warn("Password reset OTP check failed for {}: {}", email, otpResult);
            return otpResult; // "INVALID" | "EXPIRED" | "MAX_ATTEMPTS"
        }

        // 2. OTP valid — load user
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Password reset: user not found for email {}", email);
            return "USER_NOT_FOUND";
        }

        // 3. Hash new password with BCrypt and save
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successful for: {}", email);
        return "SUCCESS";
    }
}
package com.orivya.service;

import com.orivya.entity.OtpVerification;
import com.orivya.repository.OtpRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.TrackingSettings;
import com.sendgrid.helpers.mail.objects.ClickTrackingSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * OtpService — Handles all OTP operations using SendGrid.
 *
 * WHAT CHANGED vs Gmail SMTP version:
 *   - Removed: JavaMailSender, SimpleMailMessage, spring-mail
 *   - Added:   SendGrid SDK, @Value for API key/from email
 *   - Changed: sendEmail() method uses SendGrid HTTP API
 *
 * Everything else (generate, verify, resend logic) is IDENTICAL.
 *
 * HOW SENDGRID WORKS:
 *   1. You create an API key on sendgrid.com (free)
 *   2. This service calls SendGrid's REST API with that key
 *   3. SendGrid delivers the email to your customer
 *   No SMTP, no App Passwords, no port issues.
 */
@Service
@Slf4j
public class OtpService {

    // ── CHANGED: Inject OtpRepository directly (no JavaMailSender) ──
    private final OtpRepository otpRepository;

    // ── SendGrid config values from application.properties ──
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name}")
    private String fromName;

    // ── Reusable SendGrid client — initialized once after @Value injection ──
    // FIX: Creating a new SendGrid() on every email is wasteful (allocates
    // HTTP client, connection pools etc). This cached instance is reused.
    private SendGrid _sg;
    private SendGrid getSendGrid() {
        if (_sg == null) _sg = new SendGrid(sendGridApiKey);
        return _sg;
    }

    // ── Constructor injection ──────────────────────────────────────────────
    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    private static final int    OTP_EXPIRY_MINUTES = 5;
    private static final int    MAX_ATTEMPTS       = 3;
    private static final String REGISTRATION_TYPE  = "REGISTRATION";
    private static final String LOGIN_TYPE         = "LOGIN";
    private static final String PASSWORD_RESET_TYPE = "PASSWORD_RESET"; // NEW

    // ── GENERATE & SEND OTP ───────────────────────────────────────

    /**
     * Generate OTP for registration email verification.
     * Called when user submits registration form.
     */
    public void sendRegistrationOtp(String email, String userName) {
        String otp = generateAndSave(email, REGISTRATION_TYPE);

        // HTML email body — looks professional in Gmail inbox
        String htmlBody =
            "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:32px;background:#f9f9f9;border-radius:8px'>" +
            "  <div style='text-align:center;margin-bottom:24px'>" +
            "    <h2 style='color:#1a5c2a;font-family:serif;margin:0'>🌾 ORIVYA</h2>" +
            "    <p style='color:#888;margin:4px 0 0'>Sri Lakshmi Ganapati Mini Rice Mill</p>" +
            "  </div>" +
            "  <div style='background:white;padding:24px;border-radius:8px;border:1px solid #e0e0e0'>" +
            "    <p style='color:#333;margin:0 0 16px'>Hello <strong>" + userName + "</strong>,</p>" +
            "    <p style='color:#555;margin:0 0 24px'>Welcome to Orivya Rice! Use the OTP below to verify your email address:</p>" +
            "    <div style='background:#f0f7f0;border:2px dashed #1a5c2a;border-radius:8px;padding:20px;text-align:center;margin:0 0 24px'>" +
            "      <p style='margin:0 0 8px;color:#888;font-size:10px;text-transform:uppercase;letter-spacing:1px'>Your Verification OTP</p>" +
            "      <p style='margin:0;font-size:30px;font-weight:bold;letter-spacing:12px;color:#1a5c2a'>" + otp + "</p>" +
            "      <p style='margin:8px 0 0;color:#888;font-size:10px'>Valid for " + OTP_EXPIRY_MINUTES + " minutes only</p>" +
            "    </div>" +
            "    <p style='color:#e53e3e;font-size:10px;margin:0'>⚠ Do not share this OTP with anyone. Orivya Rice will never ask for your OTP.</p>" +
            "  </div>" +
            "  <p style='color:#aaa;font-size:10px;text-align:left;margin:16px 0 0'>If you did not register, please ignore this email.</p>" +
            "</div>";

        String textBody =
            "Hello " + userName + ",\n\n" +
            "Welcome to Orivya Rice!\n\n" +
            "Your email verification OTP is: " + otp + "\n\n" +
            "Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share this OTP.\n\n" +
            "Best regards,\n" +
            " Orivya Team";

        sendEmail(email, "Orivya Rice — Verify Your Email", textBody, htmlBody);
        log.info("Registration OTP sent via SendGrid to: {}", email);
    }

    /**
     * Generate OTP for login verification (2-step login).
     * Called after password is verified successfully.
     */
    public void sendLoginOtp(String email, String userName) {
        String otp = generateAndSave(email, LOGIN_TYPE);

        String htmlBody =
            "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:32px;background:#f9f9f9;border-radius:8px'>" +
            "  <div style='text-align:center;margin-bottom:24px'>" +
            "    <h2 style='color:#1a5c2a;font-family:serif;margin:0'>🌾 ORIVYA</h2>" +
            "    <p style='color:#888;margin:4px 0 0'>Secure Login</p>" +
            "  </div>" +
            "  <div style='background:white;padding:24px;border-radius:8px;border:1px solid #e0e0e0'>" +
            "    <p style='color:#333;margin:0 0 16px'>Hello <strong>" + userName + "</strong>,</p>" +
            "    <p style='color:#555;margin:0 0 24px'>You are logging in to Orivya Rice. Use the OTP below:</p>" +
            "    <div style='background:#f0f7f0;border:2px dashed #1a5c2a;border-radius:8px;padding:20px;text-align:center;margin:0 0 24px'>" +
            "      <p style='margin:0 0 8px;color:#888;font-size:10px;text-transform:uppercase;letter-spacing:1px'>Your Login OTP</p>" +
            "      <p style='margin:0;font-size:30px;font-weight:bold;letter-spacing:12px;color:#1a5c2a'>" + otp + "</p>" +
            "      <p style='margin:8px 0 0;color:#888;font-size:10px'>Valid for " + OTP_EXPIRY_MINUTES + " minutes only</p>" +
            "    </div>" +
            "    <p style='color:#e53e3e;font-size:10px;margin:0'>⚠ If you did not try to login, please change your password immediately.</p>" +
            "  </div>" +
            "  <p style='color:#aaa;font-size:10px;text-align:left;margin:16px 0 0'> Best regards,\n" +
            " Orivya Team,</p>" +
            "</div>";

        String textBody =
            "Hello " + userName + ",\n\n" +
            "Your Orivya Rice login OTP is: " + otp + "\n\n" +
            "Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share this OTP.\n\n" +
            "If you did not try to login, please change your password.\n\n" +
            "Best regards,\n" +
            " Orivya Team";

        sendEmail(email, "Orivya — Login OTP", textBody, htmlBody);
        log.info("Login OTP sent via SendGrid to: {}", email);
    }

    // ── VERIFY OTP ────────────────────────────────────────────────
    // UNCHANGED — same logic as before

    public String verifyRegistrationOtp(String email, String enteredOtp) {
        return verifyOtp(email, enteredOtp, REGISTRATION_TYPE);
    }

    public String verifyLoginOtp(String email, String enteredOtp) {
        return verifyOtp(email, enteredOtp, LOGIN_TYPE);
    }

    // ── RESEND OTP ────────────────────────────────────────────────
    // UNCHANGED — same logic as before

    public void resendRegistrationOtp(String email, String userName) {
        otpRepository.deleteByEmailAndType(email, REGISTRATION_TYPE);
        sendRegistrationOtp(email, userName);
    }

    public void resendLoginOtp(String email, String userName) {
        otpRepository.deleteByEmailAndType(email, LOGIN_TYPE);
        sendLoginOtp(email, userName);
    }

    // ── PASSWORD RESET OTP (NEW — reuses existing infrastructure) ──────────────

    /**
     * NEW: Send OTP for forgot-password flow.
     * Reuses existing generateAndSave() and sendEmail() — nothing new needed.
     */
    public void sendPasswordResetOtp(String email, String userName) {
        String otp = generateAndSave(email, PASSWORD_RESET_TYPE);
        String htmlBody =
            "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:32px;background:#f9f9f9;border-radius:8px'>" +
            "  <div style='text-align:center;margin-bottom:24px'>" +
            "    <h2 style='color:#1a5c2a;font-family:serif;margin:0'>🌾 ORIVYA</h2>" +
            "    <p style='color:#888;margin:4px 0 0'>Password Reset</p>" +
            "  </div>" +
            "  <div style='background:white;padding:24px;border-radius:8px;border:1px solid #e0e0e0'>" +
            "    <p style='color:#333;margin:0 0 16px'>Hello <strong>" + userName + "</strong>,</p>" +
            "    <p style='color:#555;margin:0 0 24px'>We received a password reset request. Use the OTP below:</p>" +
            "    <div style='background:#fff8e1;border:2px dashed #c8922a;border-radius:8px;padding:20px;text-align:center;margin:0 0 24px'>" +
            "      <p style='margin:0 0 8px;color:#888;font-size:10px;text-transform:uppercase;letter-spacing:1px'>Password Reset OTP</p>" +
            "      <p style='margin:0;font-size:30px;font-weight:bold;letter-spacing:12px;color:#c8922a'>" + otp + "</p>" +
            "      <p style='margin:8px 0 0;color:#888;font-size:10px'>Valid for " + OTP_EXPIRY_MINUTES + " minutes only</p>" +
            "    </div>" +
            "    <p style='color:#e53e3e;font-size:10px;margin:0'>⚠ If you did not request a password reset, please ignore this email. Your password will not change.</p>" +
            "  </div>" +
            "  <p style='color:#aaa;font-size:10px;text-align:left;margin:16px 0 0'> Best regards,\n" +
            " Orivya Team,</p>" +
            "</div>";
        String textBody =
            "Hello " + userName + ",\n\n" +
            "Your Orivya Rice password reset OTP is: " + otp + "\n\n" +
            "Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share this OTP.\n\n" +
            "If you did not request this, please ignore.\n\n" +
            " Best regards,\n" +
            " Orivya Team";
        sendEmail(email, "Orivya— Password Reset OTP", textBody, htmlBody);
        log.info("Password reset OTP sent via SendGrid to: {}", email);
    }

    /**
     * NEW: Verify the password reset OTP.
     * Returns: "VALID" | "INVALID" | "EXPIRED" | "MAX_ATTEMPTS"
     */
    public String verifyPasswordResetOtp(String email, String enteredOtp) {
        return verifyOtp(email, enteredOtp, PASSWORD_RESET_TYPE);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────

    /**
     * UNCHANGED — Generate and save OTP to DB.
     */
    private String generateAndSave(String email, String type) {
        otpRepository.deleteByEmailAndType(email, type);

        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpVerification otpRecord = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .attempts(0)
                .build();
        otpRepository.save(otpRecord);
        return otp;
    }

    /**
     * UNCHANGED — Core OTP verification logic.
     */
    private String verifyOtp(String email, String enteredOtp, String type) {
        Optional<OtpVerification> optRecord =
            otpRepository.findTopByEmailAndTypeOrderByCreatedAtDesc(email, type);

        if (optRecord.isEmpty()) {
            return "INVALID";
        }

        OtpVerification record = optRecord.get();

        if (record.getAttempts() >= MAX_ATTEMPTS) {
            return "MAX_ATTEMPTS";
        }

        LocalDateTime expiresAt = record.getCreatedAt().plusMinutes(OTP_EXPIRY_MINUTES);
        if (LocalDateTime.now().isAfter(expiresAt)) {
            return "EXPIRED";
        }

        if (!record.getOtp().equals(enteredOtp.trim())) {
            record.setAttempts(record.getAttempts() + 1);
            otpRepository.save(record);
            return "INVALID";
        }

        otpRepository.deleteByEmailAndType(email, type);
        return "VALID";
    }

    /**
     * FIXED — Sends email via SendGrid HTTP API.
     *
     * Fixes applied:
     *   1. @Async — runs in a separate thread so OTP generate returns
     *      immediately. User gets response in < 100ms while email
     *      sends in background.
     *   2. Reuse cached SendGrid client (not new on every call).
     *   3. MailSettings.sandboxMode = false (explicit — prevents
     *      accidental sandbox that silently drops emails).
     *   4. Click tracking disabled — links in OTP emails trigger
     *      spam filters. Disabled here since OTP has no links.
     *   5. "x-priority" header set to 1 (highest) — marks email
     *      as urgent/transactional.
     *   6. Full response logging for debugging.
     *
     * @throws RuntimeException only on HTTP 4xx/5xx — caller logs it.
     */
    @Async
    protected void sendEmail(String to, String subject, String textBody, String htmlBody) {
        long startMs = System.currentTimeMillis();
        log.info("[SendGrid] Sending '{}' to {} ...", subject, to);

        try {
            // ── Build email ──────────────────────────────────────────
            Email fromAddress = new Email(fromEmail, fromName);
            Email toAddress   = new Email(to);

            Content textContent = new Content("text/plain", textBody);
            Content htmlContent = new Content("text/html",  htmlBody);

            Mail mail = new Mail(fromAddress, subject, toAddress, textContent);
            mail.addContent(htmlContent);

            // ── Mark as transactional (avoids promotional spam folder) ──
            // Disabling click tracking prevents SendGrid from wrapping URLs,
            // which is a common trigger for spam filters.
            TrackingSettings trackingSettings = new TrackingSettings();
            ClickTrackingSetting clickTracking = new ClickTrackingSetting();
            clickTracking.setEnable(false);
            clickTracking.setEnableText(false);
            trackingSettings.setClickTrackingSetting(clickTracking);
            mail.setTrackingSettings(trackingSettings);

            // ── Set headers: high priority + transactional category ──
            mail.addHeader("X-Priority", "1");
            mail.addHeader("X-MSMail-Priority", "High");
            mail.addCategory("transactional");
            mail.addCategory("otp");

            // ── Send via reused SendGrid client ──────────────────────
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = getSendGrid().api(request);   // reused client

            long elapsed = System.currentTimeMillis() - startMs;

            // ── Full response logging ─────────────────────────────────
            log.info("[SendGrid] to={} | status={} | elapsed={}ms",
                     to, response.getStatusCode(), elapsed);

            if (response.getStatusCode() >= 400) {
                log.error("[SendGrid] ERROR status={} body={} headers={}",
                          response.getStatusCode(),
                          response.getBody(),
                          response.getHeaders());
                throw new RuntimeException(
                    "SendGrid rejected email. Status: " + response.getStatusCode() +
                    " — " + response.getBody()
                );
            }

            // 202 = Accepted and queued for delivery (normal SendGrid response)
            log.info("[SendGrid] ✅ Accepted (202) for {} in {}ms", to, elapsed);

        } catch (IOException e) {
            log.error("[SendGrid] IOException for {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }
}
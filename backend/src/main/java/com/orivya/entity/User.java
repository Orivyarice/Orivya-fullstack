package com.orivya.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "phone")
    private String phone;

    // ── OLD single address field (kept for backward compatibility) ──
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // ── NEW: Detailed address fields ──────────────────────────────
    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "street")
    private String street;

    @Column(name = "village")
    private String village;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    // ── NEW: Email verification status ────────────────────────────
    // false = registered but OTP not verified yet
    // true  = email verified, fully active account
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    public enum Role {
        ADMIN,
        CUSTOMER
    }
}
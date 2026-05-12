package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    public String name;

    @Email(message = "Enter a valid email address")
    @NotBlank(message = "Email is required")
    public String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    public String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Enter a valid 10-digit phone number")
    public String phone;

    // ── OLD single address (kept for compatibility) ──
    public String address;

    // ── NEW: Detailed address fields ──────────────────
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Enter a valid 6-digit pincode")
    public String pincode;

    @NotBlank(message = "Street is required")
    public String street;

    public String village;   // optional

    @NotBlank(message = "City is required")
    public String city;

    @NotBlank(message = "State is required")
    public String state;
}
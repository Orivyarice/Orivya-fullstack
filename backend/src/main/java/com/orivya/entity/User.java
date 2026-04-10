package com.orivya.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * User Entity — Maps to the 'users' table in MySQL.
 * Stores customer and admin account details.
 */
@Entity
@Table(name = "users")
@Data                   // Lombok: generates getters, setters, toString, equals
@NoArgsConstructor      // Lombok: generates no-arg constructor
@AllArgsConstructor     // Lombok: generates all-arg constructor
@Builder                // Lombok: enables builder pattern (User.builder().name("...").build())
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true) // email must be unique
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password; // stored as BCrypt hash, never plain text

    @Enumerated(EnumType.STRING) // saves role as text ("ADMIN" or "CUSTOMER") in DB
    @Column(nullable = false)
    private Role role;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * Roles — ADMIN can manage products/orders; CUSTOMER can shop.
     */
    public enum Role {
        ADMIN,
        CUSTOMER
    }
}

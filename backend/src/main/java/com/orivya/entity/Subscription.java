package com.orivya.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subscription — stores user rice/milk subscription plans.
 * NEW entity — does not modify any existing entity.
 * Table created automatically by ddl-auto=update.
 */
@Entity
@Table(name = "subscriptions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* FK to users table */
    // @JsonIgnoreProperties prevents Jackson from trying to serialize the LAZY
    // proxy object, which would cause "No serializer found" or stack overflow.
    // Admin API only needs userId and userName — not the full User graph.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
                           "password", "otpVerifications", "cartItems", "orders"})
    private User user;

    /* "rice" or "milk" */
    // DB column name = "product_type"  →  Java field = productType
    // @Column explicitly maps so Hibernate finds the right column.
    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    /* e.g. "5kg", "10kg", "25kg", "500ml", "1L", "2L" */
    @Column(name = "quantity", nullable = false, length = 20)
    private String quantity;

    /* "weekly", "monthly", "daily", "alternate" */
    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency;

    /* Calculated price per delivery cycle */
    @Column(name = "price", nullable = false)
    private Double price;

    /* Customer's delivery address for this subscription */
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    /* Customer's phone for delivery coordination */
    @Column(name = "phone", length = 15)
    private String phone;

    /* When deliveries start */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /* "ACTIVE" | "PAUSED" | "CANCELLED" */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
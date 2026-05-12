package com.orivya.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Entity — Maps to the 'orders' table in MySQL.
 * Represents a customer's placed order.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many orders can belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "payment_method")
    private String paymentMethod; // COD, UPI, RAZORPAY

    @Column(name = "payment_status")
    private String paymentStatus; // PENDING, PAID

    @Column(name = "transaction_id")
    private String transactionId; // Razorpay/UPI transaction ID

    // NEW: delivery charge applied to this order
    @Column(name = "delivery_charge")
    @Builder.Default
    private Double deliveryCharge = 0.0; // 0 if free, 60 if charged

    // ── DELIVERY BOY ASSIGNMENT (new — backward compatible) ───────────
    // Nullable — null means no delivery boy assigned yet
    @Column(name = "delivery_boy_id")
    private Long deliveryBoyId;

    // ASSIGNED → OUT_FOR_DELIVERY → DELIVERED
    @Column(name = "delivery_status", length = 30)
    @Builder.Default
    private String deliveryStatus = "UNASSIGNED";

    // Name stored here so admin dashboard can show it without a JOIN
    @Column(name = "delivery_boy_name")
    private String deliveryBoyName;

    // Phone stored for quick display
    @Column(name = "delivery_boy_phone", length = 15)
    private String deliveryBoyPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One order has many items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems;

    // Auto-set timestamps before saving
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Order status lifecycle:
     * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     * Any stage can go to CANCELLED
     */
    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
}
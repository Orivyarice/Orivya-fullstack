package com.orivya.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DeliveryBoy — NEW entity, does not touch any existing entity.
 * Table: delivery_boys (auto-created by ddl-auto=update)
 */
@Entity
@Table(name = "delivery_boys")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryBoy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(length = 100)
    private String email;

    /** ACTIVE or INACTIVE */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
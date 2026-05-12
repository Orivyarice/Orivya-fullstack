package com.orivya.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Product Entity — Maps to the 'products' table in MySQL.
 * Represents a rice product listed on the website.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false)
    private Double price;

    @Column(name = "image_url")
    private String imageUrl; // path to uploaded image file

    @NotBlank(message = "Weight/size is required")
    @Column(nullable = false)
    private String weight; // e.g. "26 kg", "5 kg"

    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0; // inventory count

    @Column(name = "badge")
    private String badge; // e.g. "Best Seller", "Wholesale"

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // soft delete — false = hidden from shop

    @Column(name = "category")
    private String category; // e.g. "Sona Masoori", "Raw Rice"
}

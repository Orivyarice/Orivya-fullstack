package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    @NotBlank(message = "Delivery address is required")
    public String deliveryAddress;

    @NotBlank(message = "Payment method is required")
    public String paymentMethod;

    public String transactionId;

    // NEW: distance from mill to customer (in km)
    // Frontend calculates this using Google Maps Geocoding or user input
    // If null/missing → backend defaults to 0 km (within radius = free delivery)
    public Double distanceKm;
}
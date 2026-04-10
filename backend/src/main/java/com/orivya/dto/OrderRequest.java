package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    @NotBlank(message = "Delivery address is required") public String deliveryAddress;
    @NotBlank(message = "Payment method is required") public String paymentMethod;
    public String transactionId;
}

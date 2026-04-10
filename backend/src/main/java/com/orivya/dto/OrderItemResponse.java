package com.orivya.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemResponse {
    public Long productId;
    public String productName;
    public String productImage;
    public Integer quantity;
    public Double unitPrice;
    public Double subtotal;
}

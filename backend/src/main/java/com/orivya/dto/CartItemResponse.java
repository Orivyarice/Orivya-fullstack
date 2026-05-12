package com.orivya.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemResponse {
    public Long cartItemId;
    public Long productId;
    public String productName;
    public String productImage;
    public String weight;
    public Double unitPrice;
    public Integer quantity;
    public Double subtotal;
}

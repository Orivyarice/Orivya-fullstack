package com.orivya.dto;
import lombok.*;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    public List<CartItemResponse> items;
    public Double totalAmount;
    public Integer totalItems;
}

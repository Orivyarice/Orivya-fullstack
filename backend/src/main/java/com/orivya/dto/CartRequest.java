package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class CartRequest {
    @NotNull public Long productId;
    @NotNull @Min(1) public Integer quantity;
}

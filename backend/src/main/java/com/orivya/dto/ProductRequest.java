package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductRequest {
    @NotBlank(message = "Product name is required") public String name;
    public String description;
    @NotNull @DecimalMin("0.0") public Double price;
    @NotBlank public String weight;
    public Integer stockQuantity;
    public String badge;
    public String category;
    public Boolean isActive;
}

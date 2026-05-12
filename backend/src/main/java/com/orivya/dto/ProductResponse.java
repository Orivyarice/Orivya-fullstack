package com.orivya.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {
    public Long id;
    public String name;
    public String description;
    public Double price;
    public String imageUrl;
    public String weight;
    public Integer stockQuantity;
    public String badge;
    public String category;
    public Boolean isActive;
}

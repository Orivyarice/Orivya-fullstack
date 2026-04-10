package com.orivya.dto;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    public Long orderId;
    public String customerName;
    public Double totalPrice;
    public String status;
    public String deliveryAddress;
    public String paymentMethod;
    public String paymentStatus;
    public String createdAt;
    public List<OrderItemResponse> items;
}

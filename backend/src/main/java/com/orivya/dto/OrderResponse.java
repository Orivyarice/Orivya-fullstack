package com.orivya.dto;
import lombok.*;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
    // first-order discount fields (added previously)
    public Boolean discountApplied;
    public Double discountAmount;
    // NEW: delivery charge field
    public Double deliveryCharge;  // 0.0 = free delivery, 60.0 = paid delivery
    public Boolean freeDelivery;   // true = free, false = charged Rs.60
    // ── DELIVERY BOY fields (new — nullable) ──
    public Long   deliveryBoyId;
    public String deliveryBoyName;
    public String deliveryBoyPhone;
    public String deliveryStatus;  // UNASSIGNED | ASSIGNED | OUT_FOR_DELIVERY | DELIVERED
}
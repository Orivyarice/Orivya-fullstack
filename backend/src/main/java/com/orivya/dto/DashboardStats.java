package com.orivya.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardStats {
    public long totalUsers;
    public long totalProducts;
    public long totalOrders;
    public long pendingOrders;
    public long deliveredOrders;
    public double totalRevenue;
}

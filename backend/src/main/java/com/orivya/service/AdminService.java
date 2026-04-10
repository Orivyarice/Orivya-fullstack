package com.orivya.service;

import com.orivya.dto.DashboardStats;
import com.orivya.entity.Order;
import com.orivya.repository.OrderRepository;
import com.orivya.repository.ProductRepository;
import com.orivya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DashboardStats getDashboardStats() {
        long totalUsers     = userRepository.count();
        long totalProducts  = productRepository.findByIsActiveTrue().size();
        long totalOrders    = orderRepository.count();
        long pendingOrders  = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        long deliveredOrders= orderRepository.countByStatus(Order.OrderStatus.DELIVERED);
        Double revenue      = orderRepository.getTotalRevenue();
        double totalRevenue = revenue != null ? revenue : 0.0;

        return DashboardStats.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .totalRevenue(totalRevenue)
                .build();
    }
}

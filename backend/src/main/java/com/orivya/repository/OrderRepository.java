package com.orivya.repository;

import com.orivya.entity.Order;
import com.orivya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    List<Order> findByStatus(Order.OrderStatus status);

    long countByStatus(Order.OrderStatus status);

    // Sum total revenue of delivered orders
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getTotalRevenue();
}

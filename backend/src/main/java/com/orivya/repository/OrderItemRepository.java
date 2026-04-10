package com.orivya.repository;

import com.orivya.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * OrderItemRepository — DB operations for individual order line items.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Get all items belonging to a specific order
    List<OrderItem> findByOrderId(Long orderId);
}

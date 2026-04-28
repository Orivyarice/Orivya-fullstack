package com.orivya.service;

import com.orivya.entity.DeliveryBoy;
import com.orivya.entity.Order;
import com.orivya.repository.DeliveryBoyRepository;
import com.orivya.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DeliveryBoyService — NEW, does not touch any existing service.
 *
 * Handles:
 *   - CRUD for delivery boys
 *   - Assigning a delivery boy to an order
 *   - Delivery boy updating delivery status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryBoyService {

    private final DeliveryBoyRepository deliveryBoyRepository;
    private final OrderRepository       orderRepository;

    // ── DELIVERY BOY MANAGEMENT ────────────────────────────────────

    public DeliveryBoy addDeliveryBoy(DeliveryBoy boy) {
        boy.setStatus("ACTIVE");
        DeliveryBoy saved = deliveryBoyRepository.save(boy);
        log.info("Delivery boy added: id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public List<DeliveryBoy> getAllDeliveryBoys() {
        return deliveryBoyRepository.findAll();
    }

    /** Active delivery boys only — for admin dropdown */
    public List<DeliveryBoy> getActiveDeliveryBoys() {
        return deliveryBoyRepository.findByStatusOrderByNameAsc("ACTIVE");
    }

    public DeliveryBoy updateDeliveryBoy(Long id, DeliveryBoy updated) {
        DeliveryBoy existing = deliveryBoyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery boy not found: " + id));
        if (updated.getName()   != null) existing.setName(updated.getName());
        if (updated.getPhone()  != null) existing.setPhone(updated.getPhone());
        if (updated.getEmail()  != null) existing.setEmail(updated.getEmail());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        return deliveryBoyRepository.save(existing);
    }

    public void deleteDeliveryBoy(Long id) {
        deliveryBoyRepository.deleteById(id);
    }

    // ── ASSIGN DELIVERY BOY TO ORDER ───────────────────────────────

    /**
     * Admin assigns a delivery boy to an order.
     * Sets deliveryBoyId, deliveryBoyName, deliveryBoyPhone on the order.
     * Sets deliveryStatus = ASSIGNED and order status = CONFIRMED.
     */
    public Order assignDeliveryBoy(Long orderId, Long deliveryBoyId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        DeliveryBoy boy = deliveryBoyRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new RuntimeException("Delivery boy not found: " + deliveryBoyId));

        order.setDeliveryBoyId(boy.getId());
        order.setDeliveryBoyName(boy.getName());
        order.setDeliveryBoyPhone(boy.getPhone());
        order.setDeliveryStatus("ASSIGNED");

        // Also update order status to CONFIRMED if it was PENDING
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
        }

        Order saved = orderRepository.save(order);
        log.info("Order #{} assigned to delivery boy #{} ({})", orderId, deliveryBoyId, boy.getName());
        return saved;
    }

    // ── DELIVERY BOY UPDATES DELIVERY STATUS ───────────────────────

    /**
     * Delivery boy updates their delivery status.
     * ASSIGNED → OUT_FOR_DELIVERY → DELIVERED
     *
     * When DELIVERED: also sets order status = DELIVERED automatically.
     */
    public Order updateDeliveryStatus(Long orderId, Long deliveryBoyId, String newDeliveryStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Verify this delivery boy owns this order
        if (!deliveryBoyId.equals(order.getDeliveryBoyId())) {
            throw new RuntimeException("This order is not assigned to you.");
        }

        // Validate transition
        List<String> validStatuses = List.of("ASSIGNED", "OUT_FOR_DELIVERY", "DELIVERED");
        if (!validStatuses.contains(newDeliveryStatus)) {
            throw new RuntimeException("Invalid delivery status: " + newDeliveryStatus);
        }

        order.setDeliveryStatus(newDeliveryStatus);

        // Auto-update order status when delivered
        if ("OUT_FOR_DELIVERY".equals(newDeliveryStatus)) {
            order.setStatus(Order.OrderStatus.SHIPPED);
        }
        if ("DELIVERED".equals(newDeliveryStatus)) {
            order.setStatus(Order.OrderStatus.DELIVERED);
        }

        Order saved = orderRepository.save(order);
        log.info("Order #{} delivery status → {} by boy #{}", orderId, newDeliveryStatus, deliveryBoyId);
        return saved;
    }

    // ── DELIVERY BOY: VIEW THEIR ORDERS ───────────────────────────

    /**
     * Get all orders assigned to a specific delivery boy.
     * Only returns ASSIGNED and OUT_FOR_DELIVERY (not completed ones).
     */
    public List<Order> getOrdersForDeliveryBoy(Long deliveryBoyId) {
        // Verify delivery boy exists
        deliveryBoyRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new RuntimeException("Delivery boy not found: " + deliveryBoyId));

        return orderRepository.findByDeliveryBoyId(deliveryBoyId);
    }
}
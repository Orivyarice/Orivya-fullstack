package com.orivya.service;

import com.orivya.dto.*;
import com.orivya.entity.*;
import com.orivya.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderService — Handles order placement and management.
 *
 * Place Order flow:
 *  1. Load user's cart items
 *  2. Validate cart is not empty
 *  3. Create Order record
 *  4. Create OrderItem records (one per cart product)
 *  5. Reduce stock for each product
 *  6. Clear the cart
 *  7. Return order summary
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;


    // ── PLACE ORDER ──────────────────────────────────────

    /**
     * Place a new order from the customer's current cart.
     */
    @Transactional
    public OrderResponse placeOrder(String userEmail, OrderRequest request) {
        User user = getUserByEmail(userEmail);

        // 1. Get all cart items for this user
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cannot place order — cart is empty!");
        }

        // 2. Calculate total price
        double totalPrice = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        // NEW: FIRST ORDER DISCOUNT
        // If user has 0 previous orders AND cart >= Rs.50, deduct Rs.50
        final double FIRST_ORDER_DISCOUNT = 50.0;
        long previousOrderCount = orderRepository.countByUser(user);
        boolean isFirstOrder    = (previousOrderCount == 0);
        double discountAmount   = 0.0;
        if (isFirstOrder && totalPrice >= FIRST_ORDER_DISCOUNT) {
            discountAmount = FIRST_ORDER_DISCOUNT;
            totalPrice     = totalPrice - discountAmount;
        }

        // ── DELIVERY CHARGE LOGIC ────────────────────────────────
        // Rules:
        //   Cart total >= Rs.600 AND within 15 km  → FREE delivery
        //   Cart total <  Rs.600 OR  beyond 15 km  → Rs.60 delivery charge
        //
        // distanceKm comes from frontend (user inputs or map picks location).
        // If frontend sends null, we default to 0 km (treat as local = free).
        final double FREE_DELIVERY_MIN_AMOUNT = 600.0;
        final double MAX_FREE_DELIVERY_KM     = 15.0;
        final double DELIVERY_CHARGE          = 60.0;

        double distanceKm     = (request.getDistanceKm() != null) ? request.getDistanceKm() : 0.0;
        double cartTotal      = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum(); // original cart total before any discount

        boolean withinRadius  = (distanceKm <= MAX_FREE_DELIVERY_KM);
        boolean qualifiesFree = (cartTotal >= FREE_DELIVERY_MIN_AMOUNT) && withinRadius;
        double deliveryCharge = qualifiesFree ? 0.0 : DELIVERY_CHARGE;

        // Add delivery charge to the final total (after discount already applied)
        totalPrice = totalPrice + deliveryCharge;
        // ─────────────────────────────────────────────────────────

        // 3. Create the Order record
        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(Order.OrderStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(
                    request.getPaymentMethod().equals("COD") ? "PENDING" : "PAID"
                )
                .transactionId(request.getTransactionId())
                .deliveryCharge(deliveryCharge)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 4. Create OrderItem records from cart items
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            double subtotal = product.getPrice() * cartItem.getQuantity();

            // Reduce stock quantity
            int newStock = product.getStockQuantity() - cartItem.getQuantity();
            product.setStockQuantity(Math.max(0, newStock)); // don't go below 0
            productRepository.save(product);

            return OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        // Set items on order
        savedOrder.setOrderItems(orderItems);
        orderRepository.save(savedOrder);

        // 5. Clear the cart after successful order
        cartItemRepository.deleteByUser(user);

        // 6. Return order response with discount + delivery charge info
        OrderResponse response = mapToOrderResponse(savedOrder);
        response.setDiscountApplied(isFirstOrder && discountAmount > 0);
        response.setDiscountAmount(discountAmount);
        response.setDeliveryCharge(deliveryCharge);
        response.setFreeDelivery(qualifiesFree);

        return response;
    }

    // ── CUSTOMER: VIEW ORDER HISTORY ──────────────────────────────────────

    /**
     * Get all orders for the logged-in customer (newest first).
     * FIX: @Transactional(readOnly=true) keeps Hibernate session open so
     * order.getUser().getName(), order.getOrderItems(), item.getProduct()
     * can be accessed without LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single order by ID (customer can only see their own).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String userEmail, Long orderId) {
        User user = getUserByEmail(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Security: customer can only view their own orders
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this order.");
        }

        return mapToOrderResponse(order);
    }

    // ── ADMIN: VIEW ALL ORDERS ──────────────────────────────────────

    /**
     * Get all orders in the system (Admin only).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update order status (Admin only).
     * e.g. PENDING → CONFIRMED → SHIPPED → DELIVERED
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        try {
            order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status +
                ". Valid values: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
        }

        Order updated = orderRepository.save(order);
        return mapToOrderResponse(updated);
    }

    /**
     * Get orders filtered by status (Admin only).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus)
                    .stream()
                    .map(this::mapToOrderResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
    }

    // ── PUBLIC HELPER: get single order as OrderResponse ────────────
    /** Used by DeliveryBoyController to return full response after assign/update */
    @Transactional(readOnly = true)
    public OrderResponse getOrderResponseById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return mapToOrderResponse(order);
    }

    // ── NEW: NOTIFICATION ENDPOINTS ───────────────────────────────

    /**
     * Returns the highest order ID in the database.
     * Called by GET /api/orders/admin/latest-order-id every 8 seconds.
     * Returns 0 if no orders exist yet.
     */
    @Transactional(readOnly = true)
    public long getLatestOrderId() {
        return orderRepository.findAll(
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "id"))
            .stream()
            .findFirst()
            .map(Order::getId)
            .orElse(0L);
    }

    /**
     * Returns total number of orders in DB.
     */
    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    /**
     * Returns the full OrderResponse of the most recently placed order.
     * Called once when new order is detected — populates the toast popup.
     */
    @Transactional(readOnly = true)
    public OrderResponse getLatestOrder() {
        return orderRepository.findAll(
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "id"))
            .stream()
            .findFirst()
            .map(this::mapToOrderResponse)
            .orElse(null);
    }

    // ── HELPERS ──────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Convert Order entity → OrderResponse DTO.
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = null;

        if (order.getOrderItems() != null) {
            itemResponses = order.getOrderItems().stream()
                    .map(item -> OrderItemResponse.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .productImage(item.getProduct().getImageUrl())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .build())
                    .collect(Collectors.toList());
        }

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerName(order.getUser().getName())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt() != null
                        ? order.getCreatedAt().toString() : "")
                .items(itemResponses)
                .deliveryCharge(order.getDeliveryCharge() != null ? order.getDeliveryCharge() : 0.0)
                .freeDelivery(order.getDeliveryCharge() == null || order.getDeliveryCharge() == 0.0)
                // ── DELIVERY BOY fields (new — null-safe) ──
                .deliveryBoyId(order.getDeliveryBoyId())
                .deliveryBoyName(order.getDeliveryBoyName())
                .deliveryBoyPhone(order.getDeliveryBoyPhone())
                .deliveryStatus(order.getDeliveryStatus() != null
                        ? order.getDeliveryStatus() : "UNASSIGNED")
                .build();
    }
}
package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.dto.OrderRequest;
import com.orivya.dto.OrderResponse;
import com.orivya.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OrderController — Order placement and management API.
 *
 * Customer endpoints:
 *  POST /api/orders          → Place a new order from cart
 *  GET  /api/orders/my       → View my order history
 *  GET  /api/orders/my/{id}  → View a specific order
 *
 * Admin endpoints:
 *  GET  /api/orders/admin/all        → All orders in the system
 *  PUT  /api/orders/admin/{id}/status → Update order status
 *  GET  /api/orders/admin/status     → Filter orders by status
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    // ── CUSTOMER ──────────────────────────────────────

    /**
     * POST /api/orders
     * Place a new order from the current cart.
     *
     * Request Body:
     * {
     *   "deliveryAddress": "4-35, Gorinta, Peddapuram, AP",
     *   "paymentMethod": "COD",
     *   "transactionId": ""
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {

        try {
            OrderResponse order = orderService.placeOrder(
                    userDetails.getUsername(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(true)
                            .message("Order placed successfully! Order ID: " + order.getOrderId())
                            .data(order)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/orders/my
     * Get the logged-in customer's order history.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<OrderResponse> orders = orderService.getMyOrders(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .message("Found " + orders.size() + " orders")
                .data(orders)
                .build());
    }

    /**
     * GET /api/orders/my/{id}
     * Get details of a specific order (customer can only see their own).
     */
    @GetMapping("/my/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {

        try {
            OrderResponse order = orderService.getOrderById(
                    userDetails.getUsername(), orderId);
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .success(true)
                    .data(order)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── ADMIN ──────────────────────────────────────

    /**
     * GET /api/orders/admin/all
     * Get ALL orders (Admin dashboard).
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .message("Total orders: " + orders.size())
                .data(orders)
                .build());
    }

    /**
     * PUT /api/orders/admin/{id}/status?status=CONFIRMED
     * Update order status (Admin only).
     * Valid values: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
     */
    @PutMapping("/admin/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        try {
            OrderResponse updated = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .success(true)
                    .message("Order status updated to: " + status)
                    .data(updated)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/orders/admin/status?status=PENDING
     * Get orders filtered by status (Admin only).
     */
    @GetMapping("/admin/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @RequestParam String status) {

        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .data(orders)
                .build());
    }
}

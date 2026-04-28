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
import java.util.Map;

/**
 * OrderController — Order placement and management API.
 *
 * Customer endpoints:
 *  POST /api/orders          → Place a new order from cart
 *  GET  /api/orders/my       → View my order history
 *  GET  /api/orders/my/{id}  → View a specific order
 *
 * Admin endpoints:
 *  GET  /api/orders/admin/all              → All orders in the system
 *  PUT  /api/orders/admin/{id}/status      → Update order status
 *  GET  /api/orders/admin/status           → Filter orders by status
 *  GET  /api/orders/admin/latest-order-id  → NEW: latest order ID for notifications
 *  GET  /api/orders/admin/latest-order     → NEW: full details of latest order
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

    // ── NEW: REAL-TIME NOTIFICATION ENDPOINTS ──────────────────────────────

    /**
     * GET /api/orders/admin/latest-order-id
     *
     * Returns ONLY the latest order's ID and total order count.
     * Called every 8 seconds by admin dashboard notification poller.
     *
     * Why this endpoint instead of reusing /admin/all:
     *   - /admin/all fetches every order with all items — heavy payload
     *   - This returns { "latestId": 42, "totalCount": 42 } — ~40 bytes
     *   - Zero impact on database performance
     *   - Allows frontend to detect new orders without downloading everything
     */
    @GetMapping("/admin/latest-order-id")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLatestOrderId() {
        try {
            long latestId    = orderService.getLatestOrderId();
            long totalCount  = orderService.getTotalOrderCount();
            return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                    .success(true)
                    .data(Map.of(
                        "latestId",   latestId,
                        "totalCount", totalCount
                    ))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/orders/admin/latest-order
     *
     * Returns the full details of the most recent order.
     * Called ONCE when a new order is detected, to show in the toast popup.
     */
    @GetMapping("/admin/latest-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getLatestOrder() {
        try {
            OrderResponse order = orderService.getLatestOrder();
            if (order == null) {
                return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                        .success(false).message("No orders yet.").build());
            }
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .success(true)
                    .data(order)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.dto.OrderResponse;
import com.orivya.entity.DeliveryBoy;
import com.orivya.entity.Order;
import com.orivya.service.DeliveryBoyService;
import com.orivya.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeliveryBoyController — NEW file, does not modify any existing controller.
 *
 * ── ADMIN endpoints (hasRole ADMIN) ──
 *   POST   /api/delivery/boys                     → add new delivery boy
 *   GET    /api/delivery/boys                     → all delivery boys
 *   GET    /api/delivery/boys/active              → active only (for dropdown)
 *   PUT    /api/delivery/boys/{id}                → edit delivery boy
 *   DELETE /api/delivery/boys/{id}                → delete delivery boy
 *   POST   /api/delivery/assign                   → assign boy to order
 *
 * ── DELIVERY BOY endpoints (any authenticated user) ──
 *   GET    /api/delivery/orders/{deliveryBoyId}   → boy's assigned orders
 *   POST   /api/delivery/update-status            → boy updates delivery status
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeliveryBoyController {

    private final DeliveryBoyService deliveryBoyService;
    private final OrderService       orderService;

    // ══════════════════════════════════════════════════════════
    // ADMIN — DELIVERY BOY MANAGEMENT
    // ══════════════════════════════════════════════════════════

    /** POST /api/delivery/boys — Add a new delivery boy */
    @PostMapping("/boys")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoy>> addBoy(
            @RequestBody DeliveryBoy boy) {
        try {
            DeliveryBoy saved = deliveryBoyService.addDeliveryBoy(boy);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<DeliveryBoy>builder()
                            .success(true).message("Delivery boy added.")
                            .data(saved).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<DeliveryBoy>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /** GET /api/delivery/boys — All delivery boys */
    @GetMapping("/boys")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DeliveryBoy>>> getAllBoys() {
        List<DeliveryBoy> boys = deliveryBoyService.getAllDeliveryBoys();
        return ResponseEntity.ok(ApiResponse.<List<DeliveryBoy>>builder()
                .success(true).data(boys).build());
    }

    /** GET /api/delivery/boys/active — Active delivery boys for dropdown */
    @GetMapping("/boys/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DeliveryBoy>>> getActiveBoys() {
        List<DeliveryBoy> boys = deliveryBoyService.getActiveDeliveryBoys();
        return ResponseEntity.ok(ApiResponse.<List<DeliveryBoy>>builder()
                .success(true).data(boys).build());
    }

    /** PUT /api/delivery/boys/{id} — Edit delivery boy */
    @PutMapping("/boys/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoy>> updateBoy(
            @PathVariable Long id,
            @RequestBody DeliveryBoy updated) {
        try {
            DeliveryBoy boy = deliveryBoyService.updateDeliveryBoy(id, updated);
            return ResponseEntity.ok(ApiResponse.<DeliveryBoy>builder()
                    .success(true).message("Updated.").data(boy).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<DeliveryBoy>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /** DELETE /api/delivery/boys/{id} — Remove delivery boy */
    @DeleteMapping("/boys/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBoy(@PathVariable Long id) {
        try {
            deliveryBoyService.deleteDeliveryBoy(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Deleted.").build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ══════════════════════════════════════════════════════════
    // ADMIN — ASSIGN DELIVERY BOY TO ORDER
    // ══════════════════════════════════════════════════════════

    /**
     * POST /api/delivery/assign
     * Body: { "orderId": 1, "deliveryBoyId": 2 }
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDelivery(
            @RequestBody Map<String, Long> body) {
        try {
            Long orderId       = body.get("orderId");
            Long deliveryBoyId = body.get("deliveryBoyId");
            if (orderId == null || deliveryBoyId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<OrderResponse>builder()
                                .success(false)
                                .message("orderId and deliveryBoyId are required.")
                                .build());
            }
            Order order = deliveryBoyService.assignDeliveryBoy(orderId, deliveryBoyId);
            OrderResponse resp = orderService.getOrderResponseById(order.getId());
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .success(true)
                    .message("Delivery boy assigned to order #" + orderId)
                    .data(resp).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELIVERY BOY — VIEW & UPDATE THEIR ORDERS
    // ══════════════════════════════════════════════════════════

    /**
     * GET /api/delivery/orders/{deliveryBoyId}
     * Delivery boy fetches their assigned orders.
     */
    @GetMapping("/orders/{deliveryBoyId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getDeliveryOrders(
            @PathVariable Long deliveryBoyId) {
        try {
            List<Order> orders = deliveryBoyService.getOrdersForDeliveryBoy(deliveryBoyId);
            List<OrderResponse> responses = orders.stream()
                    .map(o -> orderService.getOrderResponseById(o.getId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                    .success(true).data(responses).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<OrderResponse>>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /**
     * POST /api/delivery/update-status
     * Body: { "orderId": 1, "deliveryBoyId": 2, "status": "OUT_FOR_DELIVERY" }
     */
    @PostMapping("/update-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateDeliveryStatus(
            @RequestBody Map<String, Object> body) {
        try {
            Long   orderId       = Long.valueOf(body.get("orderId").toString());
            Long   deliveryBoyId = Long.valueOf(body.get("deliveryBoyId").toString());
            String status        = body.get("status").toString();

            Order order = deliveryBoyService.updateDeliveryStatus(orderId, deliveryBoyId, status);
            OrderResponse resp = orderService.getOrderResponseById(order.getId());
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .success(true)
                    .message("Status updated to: " + status)
                    .data(resp).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<OrderResponse>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }
}
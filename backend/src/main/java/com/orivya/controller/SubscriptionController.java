package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.entity.Subscription;
import com.orivya.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * SubscriptionController — subscription CRUD endpoints.
 * NEW file — does not modify any existing controller.
 *
 * Endpoints:
 *   POST   /api/subscription/create
 *   GET    /api/subscription/my
 *   PUT    /api/subscription/update/{id}
 *   PUT    /api/subscription/status/{id}   (pause/resume)
 *   DELETE /api/subscription/cancel/{id}
 */
@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /* ── CREATE ── */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Subscription>> create(
            @RequestBody SubscriptionService.SubscriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Subscription sub = subscriptionService.createSubscription(
                    userDetails.getUsername(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<Subscription>builder()
                            .success(true)
                            .message("Subscription created successfully!")
                            .data(sub)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Subscription>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /* ── GET MY SUBSCRIPTIONS ── */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Subscription>>> getMySubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<Subscription> subs = subscriptionService.getUserSubscriptions(
                    userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.<List<Subscription>>builder()
                    .success(true)
                    .data(subs)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<Subscription>>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /* ── UPDATE ── */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Subscription>> update(
            @PathVariable Long id,
            @RequestBody SubscriptionService.SubscriptionUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Subscription updated = subscriptionService.updateSubscription(
                    id, userDetails.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.<Subscription>builder()
                    .success(true).message("Subscription updated.")
                    .data(updated).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Subscription>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /* ── PAUSE / RESUME ── */
    @PutMapping("/status/{id}")
    public ResponseEntity<ApiResponse<Subscription>> setStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String status = body.getOrDefault("status", "ACTIVE").toUpperCase();
            Subscription sub = subscriptionService.setStatus(id, userDetails.getUsername(), status);
            return ResponseEntity.ok(ApiResponse.<Subscription>builder()
                    .success(true).message("Status updated to: " + status)
                    .data(sub).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Subscription>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /* ── CANCEL (user) ── */
    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            subscriptionService.cancelSubscription(id, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Subscription cancelled.").build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    // ══════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS  —  require ROLE_ADMIN (JWT)
    // ══════════════════════════════════════════════════════════

    /**
     * GET /api/subscription/all
     * Returns ALL subscriptions in the system (admin only).
     * This is the endpoint the admin dashboard calls.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Subscription>>> getAllSubscriptions() {
        try {
            List<Subscription> subs = subscriptionService.getAllSubscriptions();
            return ResponseEntity.ok(ApiResponse.<List<Subscription>>builder()
                    .success(true)
                    .message("Total: " + subs.size() + " subscriptions")
                    .data(subs)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<Subscription>>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /**
     * PUT /api/subscription/admin/cancel/{id}
     * Admin cancels any subscription — no ownership check.
     */
    @PutMapping("/admin/cancel/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Subscription>> adminCancel(
            @PathVariable Long id) {
        try {
            Subscription sub = subscriptionService.adminCancelSubscription(id);
            return ResponseEntity.ok(ApiResponse.<Subscription>builder()
                    .success(true).message("Subscription #" + id + " cancelled by admin.")
                    .data(sub).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Subscription>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }

    /**
     * PUT /api/subscription/admin/update/{id}
     * Admin updates address/phone/startDate on any subscription.
     */
    @PutMapping("/admin/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Subscription>> adminUpdate(
            @PathVariable Long id,
            @RequestBody SubscriptionService.SubscriptionUpdateRequest request) {
        try {
            Subscription sub = subscriptionService.adminUpdateSubscription(id, request);
            return ResponseEntity.ok(ApiResponse.<Subscription>builder()
                    .success(true).message("Subscription #" + id + " updated.")
                    .data(sub).build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Subscription>builder()
                            .success(false).message(e.getMessage()).build());
        }
    }
}
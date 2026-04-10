package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PaymentController — Handles Razorpay payment gateway.
 *
 * Razorpay supports: GPay, PhonePe, Paytm, UPI, Cards, Net Banking
 *
 * Flow:
 * 1. Frontend calls POST /api/payments/create-order
 * 2. Backend creates Razorpay order, returns order_id
 * 3. Frontend opens Razorpay checkout (GPay/PhonePe etc.)
 * 4. After payment, frontend sends payment details to /api/payments/verify
 * 5. Backend verifies signature, marks order as PAID
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/create-order
     * Creates a Razorpay order for the current cart total.
     * Returns razorpay_order_id to open payment modal.
     */
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> orderData = paymentService.createRazorpayOrder(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Payment order created")
                    .data(orderData)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/payments/verify
     * Verifies Razorpay payment signature after successful payment.
     * If valid, places the order and marks it PAID.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @RequestBody Map<String, String> paymentData,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> result = paymentService.verifyAndPlaceOrder(
                    paymentData, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Payment verified and order placed!")
                    .data(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Payment verification failed: " + e.getMessage())
                            .build());
        }
    }
}

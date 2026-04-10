package com.orivya.service;

import com.orivya.entity.Order;
import com.orivya.repository.CartItemRepository;
import com.orivya.repository.OrderRepository;
import com.orivya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * PaymentService — Razorpay payment gateway integration.
 *
 * HOW TO GET RAZORPAY KEYS:
 * 1. Go to https://razorpay.com
 * 2. Sign up with your business details
 * 3. Dashboard → Settings → API Keys → Generate Key
 * 4. You get: Key ID (rzp_test_xxx) and Key Secret
 * 5. Add them to application.properties
 *
 * TEST CARDS (for testing):
 * Card: 4111 1111 1111 1111
 * CVV:  any 3 digits
 * Date: any future date
 * OTP:  success
 *
 * UPI TEST: success@razorpay
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderService orderService;

    // Add these to application.properties:
    // razorpay.key.id=rzp_test_YourKeyIDHere
    // razorpay.key.secret=YourKeySecretHere
    @Value("${razorpay.key.id:rzp_test_placeholder}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:placeholder_secret}")
    private String razorpayKeySecret;

    /**
     * Step 1: Create Razorpay order
     * Returns order_id, key_id, and amount to the frontend
     */
    public Map<String, Object> createRazorpayOrder(String userEmail) throws Exception {
        // Get cart total for this user
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Calculate total in paise (Razorpay uses paise, 1 rupee = 100 paise)
        double totalRupees = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
        long totalPaise = (long)(totalRupees * 100);

        // In production: use Razorpay Java SDK to create order
        // com.razorpay.RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // JSONObject options = new JSONObject();
        // options.put("amount", totalPaise);
        // options.put("currency", "INR");
        // options.put("receipt", "order_" + System.currentTimeMillis());
        // Order order = client.orders.create(options);
        // String razorpayOrderId = order.get("id");

        // For now, return the data needed for frontend Razorpay modal
        Map<String, Object> result = new HashMap<>();
        result.put("razorpayKeyId", razorpayKeyId);
        result.put("amount", totalPaise);
        result.put("currency", "INR");
        result.put("customerName", user.getName());
        result.put("customerEmail", user.getEmail());
        result.put("customerPhone", user.getPhone() != null ? user.getPhone() : "");
        // result.put("razorpayOrderId", razorpayOrderId); // uncomment with SDK

        return result;
    }

    /**
     * Step 2: Verify payment signature
     * Razorpay sends back: razorpay_payment_id, razorpay_order_id, razorpay_signature
     * We verify the signature using HMAC-SHA256
     */
    public Map<String, Object> verifyAndPlaceOrder(
            Map<String, String> paymentData, String userEmail) throws Exception {

        String paymentId = paymentData.get("razorpay_payment_id");
        String orderId   = paymentData.get("razorpay_order_id");
        String signature = paymentData.get("razorpay_signature");
        String address   = paymentData.get("deliveryAddress");

        // Verify signature: HMAC-SHA256(orderId + "|" + paymentId, keySecret)
        String payload  = orderId + "|" + paymentId;
        String expected = hmacSha256(payload, razorpayKeySecret);

        if (!expected.equals(signature)) {
            throw new RuntimeException("Payment signature verification failed");
        }

        // Signature valid → place the order
        // orderService.placeOrder(userEmail, address, "RAZORPAY", paymentId);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", paymentId);
        result.put("status", "PAID");
        result.put("message", "Payment successful via Razorpay");
        return result;
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

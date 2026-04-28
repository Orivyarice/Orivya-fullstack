package com.orivya.service;


import com.orivya.entity.Subscription;
import com.orivya.entity.User;
import com.orivya.repository.SubscriptionRepository;
import com.orivya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * SubscriptionService — all subscription business logic.
 * NEW file — does not touch any existing service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository         userRepository;

    /* ── SERVER-SIDE PRICE TABLE ─────────────────────────────────
       Prices are validated server-side so frontend cannot pass
       manipulated values. Frontend sends the UI-displayed price
       but backend ignores it and recalculates from this table.
       ──────────────────────────────────────────────────────────── */
    private static final Map<String, Double> PRICE_TABLE = new HashMap<>();

    static {
        /* Rice: qty-frequency → price */
        PRICE_TABLE.put("rice-5-monthly",    299.0);
        PRICE_TABLE.put("rice-5-weekly",      89.0);
        PRICE_TABLE.put("rice-10-monthly",   549.0);
        PRICE_TABLE.put("rice-10-weekly",    159.0);
        PRICE_TABLE.put("rice-25-monthly",  1199.0);
        PRICE_TABLE.put("rice-25-weekly",    349.0);

        /* Milk: qty-frequency → price */
        PRICE_TABLE.put("milk-500-daily",      39.0);
        PRICE_TABLE.put("milk-500-alternate",  22.0);
        PRICE_TABLE.put("milk-1L-daily",       69.0);
        PRICE_TABLE.put("milk-1L-alternate",   39.0);
        PRICE_TABLE.put("milk-2L-daily",      129.0);
        PRICE_TABLE.put("milk-2L-alternate",   72.0);
    }

    /* ── CREATE SUBSCRIPTION ──────────────────────────────────── */
    public Subscription createSubscription(String userEmail, SubscriptionRequest request) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found."));

        /* Validate product type */
        if (!"rice".equals(request.getProductType()) && !"milk".equals(request.getProductType())) {
            throw new RuntimeException("Invalid product type. Must be 'rice' or 'milk'.");
        }

        /* Validate frequency */
        String freq = request.getFrequency().toLowerCase();
        String type = request.getProductType().toLowerCase();

        if ("rice".equals(type) && !List.of("weekly","monthly").contains(freq)) {
            throw new RuntimeException("Rice subscriptions support: weekly, monthly.");
        }
        if ("milk".equals(type) && !List.of("daily","alternate").contains(freq)) {
            throw new RuntimeException("Milk subscriptions support: daily, alternate.");
        }

        /* Validate quantity */
        String qty = normaliseQty(request.getQuantity());

        /* Server-side price lookup — ignores frontend price */
        String priceKey = type + "-" + qty + "-" + freq;
        Double serverPrice = PRICE_TABLE.get(priceKey);
        if (serverPrice == null) {
            throw new RuntimeException("Invalid subscription plan: " + priceKey);
        }

        /* Validate start date */
        if (request.getStartDate() == null) {
            throw new RuntimeException("Start date is required.");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past.");
        }

        Subscription sub = Subscription.builder()
                .user(user)
                .productType(type)
                .quantity(request.getQuantity())
                .frequency(freq)
                .price(serverPrice)   /* always use server price */
                .deliveryAddress(request.getDeliveryAddress())
                .phone(request.getPhone())
                .startDate(request.getStartDate())
                .status("ACTIVE")
                .build();

        Subscription saved = subscriptionRepository.save(sub);
        log.info("Subscription created: id={} user={} type={} qty={} freq={}",
                saved.getId(), userEmail, type, qty, freq);
        return saved;
    }

    /* ── GET USER SUBSCRIPTIONS ───────────────────────────────── */
    public List<Subscription> getUserSubscriptions(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found."));
        return subscriptionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /* ── UPDATE SUBSCRIPTION ──────────────────────────────────── */
    public Subscription updateSubscription(Long id, String userEmail, SubscriptionUpdateRequest req) {
        Subscription sub = getOwnedSubscription(id, userEmail);

        if (req.getDeliveryAddress() != null) sub.setDeliveryAddress(req.getDeliveryAddress());
        if (req.getPhone() != null)           sub.setPhone(req.getPhone());
        if (req.getStartDate() != null)       sub.setStartDate(req.getStartDate());

        return subscriptionRepository.save(sub);
    }

    /* ── PAUSE / RESUME ───────────────────────────────────────── */
    public Subscription setStatus(Long id, String userEmail, String newStatus) {
        Subscription sub = getOwnedSubscription(id, userEmail);
        if (!List.of("ACTIVE","PAUSED","CANCELLED").contains(newStatus)) {
            throw new RuntimeException("Invalid status: " + newStatus);
        }
        sub.setStatus(newStatus);
        return subscriptionRepository.save(sub);
    }

    /* ── CANCEL (user — checks ownership) ─────────────────────── */
    public void cancelSubscription(Long id, String userEmail) {
        setStatus(id, userEmail, "CANCELLED");
        log.info("Subscription {} cancelled by {}", id, userEmail);
    }

    /* ── ADMIN: GET ALL SUBSCRIPTIONS ────────────────────────────
       No user filter — returns every subscription in the system.
       Called by GET /api/subscription/all (admin only).
       ──────────────────────────────────────────────────────────── */
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll(
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
            )
        );
    }

    /* ── ADMIN: CANCEL (no ownership check) ─────────────────────
       Admin can cancel any subscription regardless of owner.
       Called by PUT /api/subscription/admin/cancel/{id}.
       ──────────────────────────────────────────────────────────── */
    public Subscription adminCancelSubscription(Long id) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
        sub.setStatus("CANCELLED");
        Subscription saved = subscriptionRepository.save(sub);
        log.info("Subscription {} cancelled by ADMIN", id);
        return saved;
    }

    /* ── ADMIN: UPDATE (no ownership check) ─────────────────────
       Admin can update address / phone / startDate on any subscription.
       Called by PUT /api/subscription/admin/update/{id}.
       ──────────────────────────────────────────────────────────── */
    public Subscription adminUpdateSubscription(Long id, SubscriptionUpdateRequest req) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
        if (req.getDeliveryAddress() != null) sub.setDeliveryAddress(req.getDeliveryAddress());
        if (req.getPhone()           != null) sub.setPhone(req.getPhone());
        if (req.getStartDate()       != null) sub.setStartDate(req.getStartDate());
        return subscriptionRepository.save(sub);
    }

    /* ── HELPERS ──────────────────────────────────────────────── */

    private Subscription getOwnedSubscription(Long id, String userEmail) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

        if (!sub.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied to this subscription.");
        }
        return sub;
    }

    /** Normalise qty: "5kg"→"5", "500ml"→"500", "1L"→"1L", "25kg"→"25" */
    private String normaliseQty(String qty) {
        if (qty == null) throw new RuntimeException("Quantity is required.");
        return qty.replaceAll("kg", "").replaceAll("ml", "").trim();
    }

    /* ── INNER REQUEST DTOs ───────────────────────────────────── */

    @lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class SubscriptionRequest {
        private String    productType;
        private String    quantity;
        private String    frequency;
        private Double    price;           /* ignored — server recalculates */
        private LocalDate startDate;
        private String    deliveryAddress;
        private String    phone;
    }

    @lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class SubscriptionUpdateRequest {
        private String    deliveryAddress;
        private String    phone;
        private LocalDate startDate;
    }
}
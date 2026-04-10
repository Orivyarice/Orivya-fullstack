package com.orivya.service;

import com.orivya.dto.CartItemResponse;
import com.orivya.dto.CartRequest;
import com.orivya.dto.CartResponse;
import com.orivya.entity.CartItem;
import com.orivya.entity.Product;
import com.orivya.entity.User;
import com.orivya.repository.CartItemRepository;
import com.orivya.repository.ProductRepository;
import com.orivya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CartService — Manages shopping cart operations.
 *
 * All cart data is stored in MySQL (persistent cart).
 * This means the cart survives page refreshes and
 * is linked to the logged-in user's account.
 *
 * Operations:
 *  - Add item to cart
 *  - Update item quantity
 *  - Remove item from cart
 *  - Get full cart with total
 *  - Clear cart (called after order placed)
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // ── ADD TO CART ──────────────────────────────────────

    /**
     * Add a product to the cart.
     * If product already in cart → increase quantity.
     * If new product → create a new cart item row.
     */
    @Transactional
    public CartResponse addToCart(String userEmail, CartRequest request) {
        User user = getUserByEmail(userEmail);
        Product product = getProductById(request.getProductId());

        // Check if this product is already in the user's cart
        Optional<CartItem> existingItem =
                cartItemRepository.findByUserAndProduct(user, product);

        if (existingItem.isPresent()) {
            // Product already in cart — just increase quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // New product — create a new cart item
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        // Return the updated full cart
        return getCart(userEmail);
    }

    // ── GET CART ──────────────────────────────────────

    /**
     * Get all cart items for the logged-in user.
     * Calculates subtotals and grand total.
     */
    public CartResponse getCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        // Convert each CartItem entity to a response DTO
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        // Calculate grand total
        double totalAmount = itemResponses.stream()
                .mapToDouble(CartItemResponse::getSubtotal)
                .sum();

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }

    // ── UPDATE QUANTITY ──────────────────────────────────────

    /**
     * Update the quantity of a specific cart item.
     * If quantity becomes 0 or less → remove the item.
     */
    @Transactional
    public CartResponse updateCartItem(String userEmail, Long cartItemId, Integer quantity) {
        User user = getUserByEmail(userEmail);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + cartItemId));

        // Security check: make sure this cart item belongs to the logged-in user
        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this cart item.");
        }

        if (quantity <= 0) {
            // Remove item if quantity is 0
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(userEmail);
    }

    // ── REMOVE ITEM ──────────────────────────────────────

    /**
     * Remove a single item from the cart.
     */
    @Transactional
    public CartResponse removeFromCart(String userEmail, Long cartItemId) {
        User user = getUserByEmail(userEmail);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + cartItemId));

        // Security: verify ownership
        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this cart item.");
        }

        cartItemRepository.delete(item);
        return getCart(userEmail);
    }

    // ── CLEAR CART ──────────────────────────────────────

    /**
     * Remove all items from a user's cart.
     * Called automatically after an order is placed.
     */
    @Transactional
    public void clearCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        cartItemRepository.deleteByUser(user);
    }

    // ── HELPERS ──────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
    }

    /**
     * Convert CartItem entity → CartItemResponse DTO.
     */
    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        double subtotal = product.getPrice() * item.getQuantity();

        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .weight(product.getWeight())
                .unitPrice(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}

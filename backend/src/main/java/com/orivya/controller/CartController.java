package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.dto.CartRequest;
import com.orivya.dto.CartResponse;
import com.orivya.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * CartController — Shopping cart API endpoints.
 *
 * All endpoints require JWT (customer must be logged in).
 *
 *  GET    /api/cart           → Get current cart
 *  POST   /api/cart           → Add item to cart
 *  PUT    /api/cart/{itemId}  → Update item quantity
 *  DELETE /api/cart/{itemId}  → Remove item from cart
 *  DELETE /api/cart/clear     → Clear entire cart
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    /**
     * GET /api/cart
     * Get the logged-in user's cart with all items and total.
     *
     * @AuthenticationPrincipal automatically injects the logged-in user
     * from the JWT token — no need to parse the token manually.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {

        CartResponse cart = cartService.getCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cart)
                .build());
    }

    /**
     * POST /api/cart
     * Add a product to cart.
     *
     * Request Body:
     * { "productId": 1, "quantity": 2 }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartRequest request) {

        CartResponse cart = cartService.addToCart(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Item added to cart!")
                .data(cart)
                .build());
    }

    /**
     * PUT /api/cart/{cartItemId}?quantity=3
     * Update the quantity of a specific cart item.
     */
    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {

        CartResponse cart = cartService.updateCartItem(
                userDetails.getUsername(), cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Cart updated!")
                .data(cart)
                .build());
    }

    /**
     * DELETE /api/cart/{cartItemId}
     * Remove a specific item from the cart.
     */
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId) {

        CartResponse cart = cartService.removeFromCart(
                userDetails.getUsername(), cartItemId);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Item removed from cart.")
                .data(cart)
                .build());
    }

    /**
     * DELETE /api/cart/clear
     * Remove all items from the cart.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {

        cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Cart cleared.")
                .build());
    }
}

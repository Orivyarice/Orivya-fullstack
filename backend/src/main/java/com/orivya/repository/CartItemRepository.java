package com.orivya.repository;

import com.orivya.entity.CartItem;
import com.orivya.entity.Product;
import com.orivya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * CartItemRepository — DB operations for Cart.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Get all cart items for a user
    List<CartItem> findByUser(User user);

    // Find specific item in cart (to update quantity)
    Optional<CartItem> findByUserAndProduct(User user, Product product);

    // Remove all cart items for a user (after order is placed)
    void deleteByUser(User user);

    // Count items in cart (for the badge counter)
    long countByUser(User user);
}

package com.orivya.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orivya.entity.CartItem;
import com.orivya.entity.Product;
import com.orivya.entity.User;

/**
 * CartItemRepository — DB operations for Cart.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Get all cart items for a user
    List<CartItem> findByUser(User user);

    /**
     * FIX: JOIN FETCH loads Product in ONE SQL query.
     * ROOT CAUSE of "could not initialize proxy [Product#1] — no Session":
     *   CartItem.product is LAZY by default. When getCart() had no @Transactional,
     *   Hibernate session closed after findByUser(). Then accessing item.getProduct()
     *   tried to load lazy proxy on closed session → crash.
     * This query fetches CartItem + Product together — no lazy loading needed.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.user = :user"
    )
    List<CartItem> findByUserWithProduct(
        @org.springframework.data.repository.query.Param("user") User user
    );

    // Find specific item in cart (to update quantity)
    Optional<CartItem> findByUserAndProduct(User user, Product product);

    // Remove all cart items for a user (after order is placed)
    void deleteByUser(User user);

    // Count items in cart (for the badge counter)
    long countByUser(User user);
}

package com.orivya.repository;

import com.orivya.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ProductRepository — DB operations for Products.
 * Includes search and pagination support.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Get only active (visible) products for the shop
    List<Product> findByIsActiveTrue();

    // Paginated list of active products (for shop page)
    Page<Product> findByIsActiveTrue(Pageable pageable);

    // Search by name or description (case-insensitive)
    // %:keyword% is a LIKE query — matches anywhere in the string
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    // Filter by category
    Page<Product> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
}

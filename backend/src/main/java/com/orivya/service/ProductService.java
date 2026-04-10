package com.orivya.service;

import com.orivya.dto.ProductRequest;
import com.orivya.dto.ProductResponse;
import com.orivya.entity.Product;
import com.orivya.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProductService — Business logic for product management.
 *
 * Features:
 * - Create / Update / Delete products (Admin only)
 * - List products with pagination
 * - Search products by keyword
 * - Upload product images (saved to /uploads folder)
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ── CREATE ──────────────────────────────────────────

    /**
     * Create a new product (with optional image upload).
     */
    public ProductResponse createProduct(ProductRequest request, MultipartFile imageFile) {
        // 1. Handle image upload
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = saveImage(imageFile);
        }

        // 2. Build product entity
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .weight(request.getWeight())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .badge(request.getBadge())
                .category(request.getCategory())
                .imageUrl(imageUrl)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        // 3. Save to DB
        Product saved = productRepository.save(product);

        return mapToResponse(saved);
    }

    // ── READ ──────────────────────────────────────────

    /**
     * Get all active products (for shop page) — paginated.
     * page: 0-based page number, size: items per page
     */
    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return productRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Search products by keyword — searches name and description.
     */
    public Page<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.searchProducts(keyword, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get a single product by ID.
     */
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    /**
     * Get all products for admin (including inactive ones).
     */
    public List<ProductResponse> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── UPDATE ──────────────────────────────────────────

    /**
     * Update an existing product.
     */
    public ProductResponse updateProduct(Long id, ProductRequest request, MultipartFile imageFile) {
        // Find existing product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Update fields
        if (request.getName() != null)          product.setName(request.getName());
        if (request.getDescription() != null)   product.setDescription(request.getDescription());
        if (request.getPrice() != null)         product.setPrice(request.getPrice());
        if (request.getWeight() != null)        product.setWeight(request.getWeight());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getBadge() != null)         product.setBadge(request.getBadge());
        if (request.getCategory() != null)      product.setCategory(request.getCategory());
        if (request.getIsActive() != null)      product.setIsActive(request.getIsActive());

        // Handle new image upload
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageUrl(saveImage(imageFile));
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    // ── DELETE ──────────────────────────────────────────

    /**
     * Soft delete — sets isActive = false instead of removing from DB.
     * This preserves order history.
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    // ── IMAGE UPLOAD ──────────────────────────────────────────

    /**
     * Save an uploaded image file to the uploads directory.
     * Returns the relative URL path to the saved file.
     */
    private String saveImage(MultipartFile file) {
        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename to avoid overwrites
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Save the file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL path (frontend will use this to display image)
            return "/uploads/" + uniqueFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store image: " + e.getMessage());
        }
    }

    // ── MAPPER ──────────────────────────────────────────

    /**
     * Convert Product entity to ProductResponse DTO.
     */
    public ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .weight(product.getWeight())
                .stockQuantity(product.getStockQuantity())
                .badge(product.getBadge())
                .category(product.getCategory())
                .isActive(product.getIsActive())
                .build();
    }
}

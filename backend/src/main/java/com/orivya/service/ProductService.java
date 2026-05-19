package com.orivya.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orivya.dto.ProductRequest;
import com.orivya.dto.ProductResponse;
import com.orivya.entity.Product;
import com.orivya.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final Cloudinary cloudinary; // injected from CloudinaryConfig

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
     * Upload image to Cloudinary — permanent cloud storage.
     *
     * WHY CLOUDINARY:
     *   Render free tier has ephemeral filesystem. Any file saved to
     *   disk is deleted on every restart/redeploy. This caused images
     *   to disappear after logout/login.
     *   Cloudinary stores permanently in cloud — survives restarts.
     *
     * RETURNS: Full Cloudinary HTTPS URL like:
     *   https://res.cloudinary.com/your-cloud/image/upload/v123/abc.jpg
     *   This URL is stored in DB and works forever.
     */
    private String saveImage(MultipartFile file) {
        try {
            log.info("[ProductService] Uploading image to Cloudinary: {}", file.getOriginalFilename());

            // Upload to Cloudinary with auto-format and quality optimization
            Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder",         "orivya-products",  // organized in folder
                    "resource_type",  "image",
                    "quality",        "auto",             // auto compression
                    "fetch_format",   "auto"              // serve WebP to browsers
                )
            );

            // Cloudinary returns permanent HTTPS URL
            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("[ProductService] Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("[ProductService] Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
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
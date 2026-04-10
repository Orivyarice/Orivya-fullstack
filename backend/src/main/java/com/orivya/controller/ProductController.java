package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.dto.ProductRequest;
import com.orivya.dto.ProductResponse;
import com.orivya.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ProductController — REST API for products.
 *
 * Public (no login needed):
 *  GET  /api/products              → List all products (paginated)
 *  GET  /api/products/{id}         → Get single product
 *  GET  /api/products/search       → Search by keyword
 *
 * Admin only:
 *  POST   /api/products            → Create product (with image upload)
 *  PUT    /api/products/{id}       → Update product
 *  DELETE /api/products/{id}       → Delete product
 *  GET    /api/products/admin/all  → All products including inactive
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    // ── PUBLIC ENDPOINTS ──────────────────────────────────────

    /**
     * GET /api/products?page=0&size=8&sortBy=price
     * Get all active products with pagination.
     *
     * Query params:
     *  page   - page number (0 = first page)
     *  size   - items per page (default 8)
     *  sortBy - field to sort by (default "id")
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Page<ProductResponse> products = productService.getAllProducts(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder()
                .success(true)
                .message("Products fetched successfully")
                .data(products)
                .build());
    }

    /**
     * GET /api/products/{id}
     * Get a single product by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        try {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                    .success(true)
                    .data(product)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProductResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/products/search?keyword=sona&page=0&size=8
     * Search products by name or description.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        Page<ProductResponse> results = productService.searchProducts(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder()
                .success(true)
                .message("Search results for: " + keyword)
                .data(results)
                .build());
    }

    // ── ADMIN ENDPOINTS ──────────────────────────────────────

    /**
     * GET /api/products/admin/all
     * Get ALL products including inactive ones (Admin dashboard).
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllForAdmin() {
        List<ProductResponse> products = productService.getAllProductsForAdmin();
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .success(true)
                .data(products)
                .build());
    }

    /**
     * POST /api/products
     * Create a new product with optional image upload.
     *
     * This uses multipart/form-data (not JSON) because we also send a file.
     * Form fields:
     *   name, description, price, weight, stockQuantity, badge, category
     *   image (file)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestPart("product") @Valid ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            ProductResponse created = productService.createProduct(request, imageFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<ProductResponse>builder()
                            .success(true)
                            .message("Product created successfully!")
                            .data(created)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProductResponse>builder()
                            .success(false)
                            .message("Failed to create product: " + e.getMessage())
                            .build());
        }
    }

    /**
     * PUT /api/products/{id}
     * Update an existing product.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            ProductResponse updated = productService.updateProduct(id, request, imageFile);
            return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                    .success(true)
                    .message("Product updated successfully!")
                    .data(updated)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProductResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * DELETE /api/products/{id}
     * Soft-delete a product (sets isActive = false).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Product deleted successfully.")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}

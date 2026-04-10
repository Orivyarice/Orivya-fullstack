package com.orivya.controller;

import com.orivya.dto.ApiResponse;
import com.orivya.dto.DashboardStats;
import com.orivya.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AdminController — Admin dashboard API.
 *
 * All endpoints require ADMIN role.
 *
 *  GET /api/admin/dashboard  → Dashboard statistics
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // All endpoints in this controller require ADMIN
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /api/admin/dashboard
     * Returns key statistics for the admin dashboard:
     * - Total users, products, orders
     * - Pending & delivered order counts
     * - Total revenue
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboard() {
        DashboardStats stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.<DashboardStats>builder()
                .success(true)
                .message("Dashboard stats loaded")
                .data(stats)
                .build());
    }
}

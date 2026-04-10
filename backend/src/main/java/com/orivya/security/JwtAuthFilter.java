package com.orivya.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter — Intercepts every HTTP request and validates the JWT token.
 *
 * CIRCULAR DEPENDENCY FIX:
 * ─────────────────────────────────────────────────────
 * OLD (broken): injected UserDetailsService interface
 *   → Spring looked for a @Bean of type UserDetailsService
 *   → Found it inside SecurityConfig
 *   → But SecurityConfig needed JwtAuthFilter first → LOOP!
 *
 * NEW (fixed): injects CustomUserDetailsService directly
 *   → CustomUserDetailsService is a standalone @Service
 *   → No dependency on SecurityConfig at all
 *   → Loop broken ✅
 * ─────────────────────────────────────────────────────
 *
 * How JWT auth works per request:
 *   1. Read "Authorization: Bearer <token>" header
 *   2. Extract email from token
 *   3. Load user from database
 *   4. Validate token (not expired, signature matches)
 *   5. Set user as authenticated in Spring Security context
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // ✅ FIX: Use CustomUserDetailsService directly, NOT UserDetailsService interface
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header or wrong format → skip JWT check
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the token (strip "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        try {
            // Extract email from token payload
            final String userEmail = jwtUtil.extractUsername(jwt);

            // Only process if email found AND user not already authenticated
            if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details from DB using email
                UserDetails userDetails =
                    customUserDetailsService.loadUserByUsername(userEmail); // ✅

                // Validate token: check signature + expiry
                if (jwtUtil.isTokenValid(jwt, userDetails)) {

                    // Create Spring Security authentication object
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities() // ROLE_ADMIN or ROLE_CUSTOMER
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Tell Spring Security this request is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Invalid/expired token — continue without authentication
            // Spring Security will block access to protected routes
            logger.warn("JWT validation failed: " + e.getMessage());
        }

        // Always continue to the next filter
        filterChain.doFilter(request, response);
    }
}

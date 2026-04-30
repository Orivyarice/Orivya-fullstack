package com.orivya.config;

import com.orivya.security.CustomUserDetailsService;
import com.orivya.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * SecurityConfig — Security rules for the application.
 *
 * CIRCULAR DEPENDENCY FIX:
 * ─────────────────────────────────────────────────────
 * OLD (broken):
 *   SecurityConfig had UserDetailsService @Bean inside it
 *   JwtAuthFilter needed UserDetailsService
 *   SecurityConfig needed JwtAuthFilter → LOOP!
 *
 * NEW (fixed):
 *   UserDetailsService moved to CustomUserDetailsService (@Service)
 *   SecurityConfig injects CustomUserDetailsService directly
 *   No more loop — all beans are independent
 * ─────────────────────────────────────────────────────
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ✅ FIX: Inject CustomUserDetailsService (separate class, no loop)
    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Main security filter chain — defines which routes are public vs protected.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for REST APIs with JWT
            .csrf(csrf -> csrf.disable())

            // Enable CORS — allow frontend to call backend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Route authorization rules
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC — no login needed ──
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()

                // ── ADMIN only ──
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                // ── SUBSCRIPTION admin endpoints ──────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/subscription/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/subscription/admin/**").hasRole("ADMIN")

                // ── DELIVERY BOY admin endpoints ───────────────────────────
                // Admin-only: manage delivery boys + assign to orders
                .requestMatchers(HttpMethod.POST,   "/api/delivery/boys").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/delivery/boys").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/delivery/boys/active").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/delivery/boys/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/delivery/boys/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/delivery/assign").hasRole("ADMIN")
                // Delivery boy endpoints: any authenticated user
                .requestMatchers("/api/delivery/orders/**").authenticated()
                .requestMatchers("/api/delivery/update-status").authenticated()

                // ── WEBHOOK: public — Twilio calls this without JWT ──
                .requestMatchers("/webhook/**").permitAll()

                // ── All other endpoints require login ──
                .anyRequest().authenticated()
            )

            // Stateless sessions — use JWT, not cookies
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Use our custom authentication provider
            .authenticationProvider(authenticationProvider())

            // Run our JWT filter before Spring's default auth filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration — allows the frontend to make API calls.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(Arrays.asList(
            // ── LOCAL DEVELOPMENT ──
            "http://localhost:3000",
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://127.0.0.1:3000",
            "http://10.*",       // LAN IP access
            "http://192.168.*",  // LAN IP access
            // ── PRODUCTION ────────────────────────────────────────────
            // UPDATE THIS: replace with your actual Netlify/Vercel URL
            "https://*.netlify.app",
            "https://*.vercel.app",
            "https://orivya-rice.netlify.app",  // replace with your URL
            "https://orivya-rice.pages.dev"      // Cloudflare Pages fallback
        ));

        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * AuthenticationProvider — connects CustomUserDetailsService + PasswordEncoder.
     * ✅ FIX: uses customUserDetailsService directly (no circular reference)
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService); // ✅ direct reference
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder — never store plain text passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager — used by AuthService to authenticate logins.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
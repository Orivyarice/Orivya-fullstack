package com.orivya.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtil — Handles JWT token creation and validation.
 *
 * JWT (JSON Web Token) = 3 parts separated by dots:
 *   HEADER.PAYLOAD.SIGNATURE
 *   - Header: algorithm type (HS256)
 *   - Payload: user data (email, role, expiry)
 *   - Signature: HMAC hash to verify token wasn't tampered
 */
@Component
public class JwtUtil {

    // Read secret key from application.properties
    @Value("${jwt.secret}")
    private String secretKey;

    // Token expiration time (default 24 hours = 86400000 ms)
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generate a JWT token for a user.
     * Called after successful login/registration.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return buildToken(claims, userDetails.getUsername(), jwtExpiration);
    }

    /**
     * Generate token with extra claims (e.g. role, userId).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails.getUsername(), jwtExpiration);
    }

    /**
     * Build and sign the JWT token.
     */
    private String buildToken(Map<String, Object> extraClaims,
                               String subject,
                               long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)          // email is the subject
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract the email (username) from a token.
     */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validate a token — checks:
     * 1. Username matches the user in DB
     * 2. Token is not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Check if the token has expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    /**
     * Create a signing key from the secret string.
     * Uses HMAC-SHA256 algorithm.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(secretKey.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

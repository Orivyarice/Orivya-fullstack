package com.orivya.repository;

import com.orivya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * UserRepository — Handles all DB operations for User.
 * JpaRepository gives us save(), findById(), findAll(), delete() for free.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA auto-generates SQL: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Check if email already exists (used during registration)
    boolean existsByEmail(String email);
}

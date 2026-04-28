package com.orivya.repository;

import com.orivya.entity.Subscription;
import com.orivya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * SubscriptionRepository — DB operations for subscriptions.
 * NEW file — does not modify any existing repository.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /* All subscriptions for a user (newest first) */
    List<Subscription> findByUserOrderByCreatedAtDesc(User user);

    /* Active subscriptions for a user */
    List<Subscription> findByUserAndStatus(User user, String status);

    /* Count active subscriptions for a user */
    long countByUserAndStatus(User user, String status);
}
package com.orivya.repository;

import com.orivya.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    // Find the latest OTP for an email + type combination
    Optional<OtpVerification> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);

    // Delete all OTPs for an email (called after successful verify)
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.email = :email AND o.type = :type")
    void deleteByEmailAndType(String email, String type);
}
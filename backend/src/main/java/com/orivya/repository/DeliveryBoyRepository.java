package com.orivya.repository;

import com.orivya.entity.DeliveryBoy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * DeliveryBoyRepository — NEW, does not touch any existing repository.
 */
@Repository
public interface DeliveryBoyRepository extends JpaRepository<DeliveryBoy, Long> {

    /** All active delivery boys — used to populate admin dropdown */
    List<DeliveryBoy> findByStatusOrderByNameAsc(String status);
}
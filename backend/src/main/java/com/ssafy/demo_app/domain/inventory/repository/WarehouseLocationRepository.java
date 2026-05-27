package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Integer> {
    Optional<WarehouseLocation> findByLocationCode(String locationCode);
    boolean existsByLocationCode(String locationCode);
    boolean existsByLocationCodeAndLocationIdNot(String locationCode, Integer locationId);
}

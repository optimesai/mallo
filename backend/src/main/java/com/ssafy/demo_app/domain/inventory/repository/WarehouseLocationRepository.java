package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Integer>,
        JpaSpecificationExecutor<WarehouseLocation> {
    Optional<WarehouseLocation> findByLocationCode(String locationCode);
    Optional<WarehouseLocation> findFirstByProductionReceiptDefaultTrueOrderByLocationIdAsc();
    Optional<WarehouseLocation> findFirstByOrderByLocationIdAsc();
    List<WarehouseLocation> findByProductionReceiptDefaultTrue();
    boolean existsByLocationCode(String locationCode);
    boolean existsByLocationCodeAndLocationIdNot(String locationCode, Integer locationId);
}

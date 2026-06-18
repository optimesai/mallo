package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentInventoryRepository extends JpaRepository<CurrentInventory, Integer>,
        JpaSpecificationExecutor<CurrentInventory> {
    Optional<CurrentInventory> findByItemAndLocation(ItemMaster item, WarehouseLocation location);
    Optional<CurrentInventory> findByItemAndLocationAndLotNumber(ItemMaster item, WarehouseLocation location, String lotNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CurrentInventory ci where ci.item = :item and ci.location = :location")
    Optional<CurrentInventory> findByItemAndLocationForUpdate(
            @Param("item") ItemMaster item,
            @Param("location") WarehouseLocation location
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CurrentInventory ci where ci.item = :item and ci.location = :location and ci.lotNumber is null")
    Optional<CurrentInventory> findByItemAndLocationAndLotNumberIsNullForUpdate(
            @Param("item") ItemMaster item,
            @Param("location") WarehouseLocation location
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CurrentInventory ci where ci.item = :item order by ci.firstInboundDate asc nulls last, ci.inventoryId asc")
    List<CurrentInventory> findByItemForUpdate(@Param("item") ItemMaster item);
    boolean existsByLocation(WarehouseLocation location);
    List<CurrentInventory> findByItem(ItemMaster item);
    List<CurrentInventory> findByItemOrderByFirstInboundDateAsc(ItemMaster item);
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    void deleteByItem(ItemMaster item);
}

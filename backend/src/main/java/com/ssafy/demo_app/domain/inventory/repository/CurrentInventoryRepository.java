package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentInventoryRepository extends JpaRepository<CurrentInventory, Integer>,
        JpaSpecificationExecutor<CurrentInventory> {
    Optional<CurrentInventory> findByItemAndLocation(ItemMaster item, WarehouseLocation location);
    boolean existsByLocation(WarehouseLocation location);
    List<CurrentInventory> findByItem(ItemMaster item);
    boolean existsByItem(ItemMaster item);
    void deleteByItem(ItemMaster item);
}

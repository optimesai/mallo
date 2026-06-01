package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransactionHistoryRepository extends JpaRepository<InventoryTransactionHistory, Integer> {
    boolean existsByItem(ItemMaster item);
    void deleteByItem(ItemMaster item);
}

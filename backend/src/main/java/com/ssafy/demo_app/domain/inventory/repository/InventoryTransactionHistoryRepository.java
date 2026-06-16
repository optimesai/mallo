package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionHistoryRepository extends JpaRepository<InventoryTransactionHistory, Integer>,
        JpaSpecificationExecutor<InventoryTransactionHistory> {
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    void deleteByItem(ItemMaster item);
    boolean existsByReasonDescContaining(String reasonDesc);
    List<InventoryTransactionHistory> findByReasonDescContainingOrderByTransactionIdAsc(String reasonDesc);
    boolean existsByWorkOrder(WorkOrder workOrder);
    List<InventoryTransactionHistory> findByWorkOrderOrderByTransactionIdAsc(WorkOrder workOrder);
    List<InventoryTransactionHistory> findByProductionExecutionOrderByTransactionIdAsc(ProductionExecution productionExecution);
    List<InventoryTransactionHistory> findTop5ByItemOrderByTransactionIdDesc(ItemMaster item);
}

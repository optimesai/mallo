package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer> {
    boolean existsByItem(ItemMaster item);
    void deleteByItem(ItemMaster item);
}

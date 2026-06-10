package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer> {
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    List<WorkOrder> findByItemOrderByOrderIdDesc(ItemMaster item);
    void deleteByItem(ItemMaster item);
    boolean existsByRouting(FactoryRouting routing);
    Optional<WorkOrder> findByOrderNo(String orderNo);
    Optional<WorkOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String orderNoPrefix);
}

package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionExecutionRepository extends JpaRepository<ProductionExecution, Integer> {
    List<ProductionExecution> findByOrderOrderByExecutionIdAsc(WorkOrder order);
    boolean existsByOrder(WorkOrder order);
}

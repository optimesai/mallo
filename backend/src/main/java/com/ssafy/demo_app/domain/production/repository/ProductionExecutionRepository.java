package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionExecutionRepository extends JpaRepository<ProductionExecution, Integer> {
    List<ProductionExecution> findByOrderOrderByExecutionIdAsc(WorkOrder order);
    boolean existsByOrder(WorkOrder order);
    boolean existsByRouting(FactoryRouting routing);
    long countByRouting(FactoryRouting routing);
    List<ProductionExecution> findTop5ByRoutingOrderByExecutionIdDesc(FactoryRouting routing);
    Optional<ProductionExecution> findTopByRoutingOrderByCreatedAtDesc(FactoryRouting routing);
}

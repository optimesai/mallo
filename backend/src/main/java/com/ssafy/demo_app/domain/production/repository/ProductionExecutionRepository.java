package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
            select r.lineName, coalesce(sum(pe.goodQty), 0), coalesce(sum(pe.defectQty), 0)
            from ProductionExecution pe
            join pe.routing r
            where pe.createdAt >= :fromDateTime
            group by r.lineName
            order by coalesce(sum(pe.goodQty), 0) + coalesce(sum(pe.defectQty), 0) desc
            """)
    List<Object[]> aggregateProductionByLine(@Param("fromDateTime") LocalDateTime fromDateTime);

    @Query("""
            select i.itemName, coalesce(sum(pe.goodQty), 0), coalesce(sum(pe.defectQty), 0)
            from ProductionExecution pe
            join pe.order wo
            join wo.item i
            where pe.createdAt >= :fromDateTime
            group by i.itemName
            order by coalesce(sum(pe.defectQty), 0) desc
            """)
    List<Object[]> aggregateQualityByItem(@Param("fromDateTime") LocalDateTime fromDateTime);

    @Query("""
            select coalesce(sum(pe.goodQty), 0), coalesce(sum(pe.defectQty), 0)
            from ProductionExecution pe
            where pe.createdAt >= :fromDateTime
            """)
    Object[] aggregateProductionTotals(@Param("fromDateTime") LocalDateTime fromDateTime);
}

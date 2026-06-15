package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer> {
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    List<WorkOrder> findByItemOrderByOrderIdDesc(ItemMaster item);
    void deleteByItem(ItemMaster item);
    boolean existsByRouting(FactoryRouting routing);
    long countByRouting(FactoryRouting routing);
    List<WorkOrder> findTop5ByRoutingOrderByOrderIdDesc(FactoryRouting routing);
    Optional<WorkOrder> findByOrderNo(String orderNo);
    Optional<WorkOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String orderNoPrefix);

    @Query("""
            select w
            from WorkOrder w
            join fetch w.item i
            join fetch w.routing r
            where (:status is null or w.status = :status)
              and (:planDate is null or w.planDate = :planDate)
              and (:fromDate is null or w.planDate >= :fromDate)
              and (:toDate is null or w.planDate <= :toDate)
              and (:keyword is null
                or lower(w.orderNo) like concat('%', :keyword, '%')
                or lower(i.itemCode) like concat('%', :keyword, '%')
                or lower(i.itemName) like concat('%', :keyword, '%'))
              and (:factoryName is null or lower(r.factoryName) like concat('%', :factoryName, '%'))
              and (:lineName is null or lower(r.lineName) like concat('%', :lineName, '%'))
            order by w.planDate desc, w.orderNo desc
            """)
    List<WorkOrder> searchWorkOrders(
            @Param("status") WorkOrder.OrderStatus status,
            @Param("planDate") LocalDate planDate,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("keyword") String keyword,
            @Param("factoryName") String factoryName,
            @Param("lineName") String lineName
    );
}

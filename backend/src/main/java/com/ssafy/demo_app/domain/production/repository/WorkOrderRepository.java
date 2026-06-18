package com.ssafy.demo_app.domain.production.repository;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String orderNoPrefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkOrder w where w.orderId = :orderId")
    Optional<WorkOrder> findByIdForUpdate(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkOrder w where w.orderNo = :orderNo")
    Optional<WorkOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Query(value = """
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
                or lower(i.itemName) like concat('%', :keyword, '%')
                or lower(r.factoryName) like concat('%', :keyword, '%')
                or lower(r.lineName) like concat('%', :keyword, '%')
                or lower(r.operationName) like concat('%', :keyword, '%'))
              and (:itemKeyword is null
                or lower(i.itemCode) like concat('%', :itemKeyword, '%')
                or lower(i.itemName) like concat('%', :itemKeyword, '%'))
              and (:factoryName is null or lower(r.factoryName) like concat('%', :factoryName, '%'))
              and (:lineName is null or lower(r.lineName) like concat('%', :lineName, '%'))
              and (:operationName is null or lower(r.operationName) like concat('%', :operationName, '%'))
            order by w.planDate desc, w.orderNo desc
            """,
            countQuery = """
            select count(w)
            from WorkOrder w
            join w.item i
            join w.routing r
            where (:status is null or w.status = :status)
              and (:planDate is null or w.planDate = :planDate)
              and (:fromDate is null or w.planDate >= :fromDate)
              and (:toDate is null or w.planDate <= :toDate)
              and (:keyword is null
                or lower(w.orderNo) like concat('%', :keyword, '%')
                or lower(i.itemCode) like concat('%', :keyword, '%')
                or lower(i.itemName) like concat('%', :keyword, '%')
                or lower(r.factoryName) like concat('%', :keyword, '%')
                or lower(r.lineName) like concat('%', :keyword, '%')
                or lower(r.operationName) like concat('%', :keyword, '%'))
              and (:itemKeyword is null
                or lower(i.itemCode) like concat('%', :itemKeyword, '%')
                or lower(i.itemName) like concat('%', :itemKeyword, '%'))
              and (:factoryName is null or lower(r.factoryName) like concat('%', :factoryName, '%'))
              and (:lineName is null or lower(r.lineName) like concat('%', :lineName, '%'))
              and (:operationName is null or lower(r.operationName) like concat('%', :operationName, '%'))
            """)
    Page<WorkOrder> searchWorkOrders(
            @Param("status") WorkOrder.OrderStatus status,
            @Param("planDate") LocalDate planDate,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("keyword") String keyword,
            @Param("itemKeyword") String itemKeyword,
            @Param("factoryName") String factoryName,
            @Param("lineName") String lineName,
            @Param("operationName") String operationName,
            Pageable pageable
    );
}

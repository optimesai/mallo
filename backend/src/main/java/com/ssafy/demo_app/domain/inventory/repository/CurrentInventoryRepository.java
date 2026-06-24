package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentInventoryRepository extends JpaRepository<CurrentInventory, Integer>,
        JpaSpecificationExecutor<CurrentInventory> {
    Optional<CurrentInventory> findByItemAndLocation(ItemMaster item, WarehouseLocation location);
    Optional<CurrentInventory> findByItemAndLocationAndLotNumber(ItemMaster item, WarehouseLocation location, String lotNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CurrentInventory ci where ci.item = :item and ci.location = :location")
    Optional<CurrentInventory> findByItemAndLocationForUpdate(
            @Param("item") ItemMaster item,
            @Param("location") WarehouseLocation location
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CurrentInventory ci where ci.item = :item and ci.location = :location and ci.lotNumber is null")
    Optional<CurrentInventory> findByItemAndLocationAndLotNumberIsNullForUpdate(
            @Param("item") ItemMaster item,
            @Param("location") WarehouseLocation location
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CurrentInventory ci where ci.item = :item order by ci.firstInboundDate asc nulls last, ci.inventoryId asc")
    List<CurrentInventory> findByItemForUpdate(@Param("item") ItemMaster item);
    boolean existsByLocation(WarehouseLocation location);
    List<CurrentInventory> findByItem(ItemMaster item);
    List<CurrentInventory> findByItemOrderByFirstInboundDateAsc(ItemMaster item);
    boolean existsByItem(ItemMaster item);
    long countByItem(ItemMaster item);
    void deleteByItem(ItemMaster item);

    @Query(value = """
            select min(ci.inventory_id) as inventoryId,
                   i.item_code as itemCode,
                   i.item_name as itemName,
                   case
                       when count(distinct wl.location_code) = 1 then max(wl.location_code)
                       when count(ci.inventory_id) = 0 then null
                       else 'MULTI'
                   end as locationCode,
                   case
                       when count(distinct wl.warehouse_name) = 1 then max(wl.warehouse_name)
                       when count(ci.inventory_id) = 0 then null
                       else '전체 창고'
                   end as warehouseName,
                   coalesce(sum(ci.current_qty), 0) as currentQty,
                   max(ci.updated_at) as updatedAt
            from item_master i
            left join current_inventory ci on ci.item_id = i.item_id
            left join warehouse_location wl on wl.location_id = ci.location_id
            where i.item_status = 'ACTIVE'
              and (:keyword is null
                or lower(coalesce(i.item_code, '')) like concat('%', lower(:keyword), '%')
                or lower(coalesce(i.item_name, '')) like concat('%', lower(:keyword), '%'))
            group by i.item_id, i.item_code, i.item_name
            order by case when i.safety_stock > coalesce(sum(ci.current_qty), 0) then 0 else 1 end,
                     max(ci.updated_at) desc,
                     i.item_code asc
            """,
            countQuery = """
                    select count(*)
                    from item_master i
                    where i.item_status = 'ACTIVE'
                      and (:keyword is null
                        or lower(coalesce(i.item_code, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(i.item_name, '')) like concat('%', lower(:keyword), '%'))
                    """,
            nativeQuery = true)
    org.springframework.data.domain.Page<InventorySummaryProjection> findInventorySummaries(
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            select wl.warehouseName, coalesce(sum(ci.currentQty), 0)
            from CurrentInventory ci
            join ci.location wl
            group by wl.warehouseName
            order by coalesce(sum(ci.currentQty), 0) desc
            """)
    List<Object[]> aggregateCurrentQtyByWarehouse();

    @Query("""
            select count(i)
            from ItemMaster i
            where i.safetyStock > (
                select coalesce(sum(ci.currentQty), 0)
                from CurrentInventory ci
                where ci.item = i
            )
            """)
    long countItemsUnderSafetyStock();
}

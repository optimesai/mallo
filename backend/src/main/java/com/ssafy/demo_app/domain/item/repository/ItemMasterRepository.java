package com.ssafy.demo_app.domain.item.repository;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemMasterRepository extends JpaRepository<ItemMaster, Integer>,
        JpaSpecificationExecutor<ItemMaster> {
    Optional<ItemMaster> findByItemCode(String itemCode);
    boolean existsByItemCode(String itemCode);
    List<ItemMaster> findByItemCodeStartingWith(String itemCodePrefix);
    List<ItemMaster> findByItemNameIgnoreCaseAndSpecAndUnit(String itemName, String spec, ItemMaster.Unit unit);
    List<ItemMaster> findByItemTypeAndItemStatus(ItemMaster.ItemType itemType, ItemMaster.ItemStatus itemStatus);
    List<ItemMaster> findByItemType(ItemMaster.ItemType itemType);
    List<ItemMaster> findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCaseOrderByItemIdAsc(
            String itemName,
            String itemCode
    );
}

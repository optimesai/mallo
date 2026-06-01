package com.ssafy.demo_app.domain.item.repository;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemMasterRepository extends JpaRepository<ItemMaster, Integer> {
    Optional<ItemMaster> findByItemCode(String itemCode);
    List<ItemMaster> findByItemTypeAndItemCodeStartingWith(ItemMaster.ItemType itemType, String itemCodePrefix);
    List<ItemMaster> findByItemType(ItemMaster.ItemType itemType);
    List<ItemMaster> findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCaseOrderByItemIdAsc(
            String itemName,
            String itemCode
    );
}

package com.ssafy.demo_app.domain.bom.repository;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomStructureRepository extends JpaRepository<BomStructure, Integer> {
    List<BomStructure> findByParentItem(ItemMaster parentItem);
    boolean existsByParentItemOrChildItem(ItemMaster parentItem, ItemMaster childItem);
    void deleteByParentItemOrChildItem(ItemMaster parentItem, ItemMaster childItem);
}

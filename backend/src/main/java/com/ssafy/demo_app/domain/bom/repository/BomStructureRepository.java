package com.ssafy.demo_app.domain.bom.repository;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomStructureRepository extends JpaRepository<BomStructure, Integer> {
    List<BomStructure> findAllByOrderByBomIdAsc();
    List<BomStructure> findByParentItem(ItemMaster parentItem);
    List<BomStructure> findByParentItemOrderByBomIdAsc(ItemMaster parentItem);
    List<BomStructure> findByParentItemAndBomVersionOrderByBomIdAsc(ItemMaster parentItem, String bomVersion);
    List<BomStructure> findByChildItemOrderByBomIdAsc(ItemMaster childItem);
    List<BomStructure> findByChildItemAndBomVersionOrderByBomIdAsc(ItemMaster childItem, String bomVersion);
    boolean existsByParentItemAndChildItemAndBomVersion(
            ItemMaster parentItem,
            ItemMaster childItem,
            String bomVersion
    );
    boolean existsByParentItemAndChildItemAndBomVersionAndBomIdNot(
            ItemMaster parentItem,
            ItemMaster childItem,
            String bomVersion,
            Integer bomId
    );
    boolean existsByParentItemOrChildItem(ItemMaster parentItem, ItemMaster childItem);
    void deleteByParentItemOrChildItem(ItemMaster parentItem, ItemMaster childItem);

    @Query("select distinct b.bomVersion from BomStructure b where b.parentItem = :parentItem order by b.bomVersion asc")
    List<String> findDistinctBomVersionsByParentItem(ItemMaster parentItem);
}

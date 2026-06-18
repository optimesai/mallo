package com.ssafy.demo_app.domain.bom.repository;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.api.bom.dto.BomGroupResponse;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomStructureRepository extends JpaRepository<BomStructure, Integer> {
    List<BomStructure> findAllByOrderByBomIdAsc();
    List<BomStructure> findByParentItem(ItemMaster parentItem);
    List<BomStructure> findByParentItemOrderByBomIdAsc(ItemMaster parentItem);
    List<BomStructure> findByParentItemAndBomStatusOrderByBomIdAsc(
            ItemMaster parentItem,
            BomStructure.BomStatus bomStatus
    );
    List<BomStructure> findByParentItemAndBomVersionOrderByBomIdAsc(ItemMaster parentItem, String bomVersion);
    List<BomStructure> findByParentItemAndBomVersionAndBomStatusOrderByBomIdAsc(
            ItemMaster parentItem,
            String bomVersion,
            BomStructure.BomStatus bomStatus
    );
    List<BomStructure> findByChildItemOrderByBomIdAsc(ItemMaster childItem);
    List<BomStructure> findByChildItemAndBomStatusOrderByBomIdAsc(
            ItemMaster childItem,
            BomStructure.BomStatus bomStatus
    );
    List<BomStructure> findByChildItemAndBomVersionOrderByBomIdAsc(ItemMaster childItem, String bomVersion);
    List<BomStructure> findByChildItemAndBomVersionAndBomStatusOrderByBomIdAsc(
            ItemMaster childItem,
            String bomVersion,
            BomStructure.BomStatus bomStatus
    );
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
    long countByParentItem(ItemMaster parentItem);
    long countByChildItem(ItemMaster childItem);
    void deleteByParentItemOrChildItem(ItemMaster parentItem, ItemMaster childItem);

    @Query("select distinct b.bomVersion from BomStructure b where b.parentItem = :parentItem order by b.bomVersion asc")
    List<String> findDistinctBomVersionsByParentItem(ItemMaster parentItem);

    @Query("select distinct b.bomVersion from BomStructure b where b.childItem = :childItem and b.bomStatus = 'ACTIVE' order by b.bomVersion asc")
    List<String> findDistinctActiveBomVersionsByChildItem(ItemMaster childItem);

    @Query("""
            select b from BomStructure b
            join fetch b.parentItem parent
            join fetch b.childItem child
            where (:parentKeyword is null
                or lower(parent.itemCode) like lower(concat('%', :parentKeyword, '%'))
                or lower(parent.itemName) like lower(concat('%', :parentKeyword, '%'))
                or str(parent.itemId) = :parentKeyword)
              and (:childKeyword is null
                or lower(child.itemCode) like lower(concat('%', :childKeyword, '%'))
                or lower(child.itemName) like lower(concat('%', :childKeyword, '%'))
                or str(child.itemId) = :childKeyword)
              and (:bomVersion is null or b.bomVersion = :bomVersion)
            order by b.bomId asc
            """)
    List<BomStructure> searchBoms(
            @Param("parentKeyword") String parentKeyword,
            @Param("childKeyword") String childKeyword,
            @Param("bomVersion") String bomVersion
    );

    @Query(
            value = """
                    select new com.ssafy.demo_app.api.bom.dto.BomGroupResponse(
                        parent.itemId,
                        parent.itemCode,
                        parent.itemName,
                        parent.itemType,
                        b.bomVersion,
                        count(b),
                        sum(case when b.bomStatus = :activeStatus then 1 else 0 end),
                        min(b.createdAt)
                    )
                    from BomStructure b
                    join b.parentItem parent
                    join b.childItem child
                    where (:parentKeyword is null
                        or lower(parent.itemCode) like lower(concat('%', :parentKeyword, '%'))
                        or lower(parent.itemName) like lower(concat('%', :parentKeyword, '%'))
                        or str(parent.itemId) = :parentKeyword)
                      and (:childKeyword is null
                        or lower(child.itemCode) like lower(concat('%', :childKeyword, '%'))
                        or lower(child.itemName) like lower(concat('%', :childKeyword, '%'))
                        or str(child.itemId) = :childKeyword)
                      and (:bomVersion is null or b.bomVersion = :bomVersion)
                    group by parent.itemId, parent.itemCode, parent.itemName, parent.itemType, b.bomVersion
                    order by min(b.bomId) asc
                    """,
            countQuery = """
                    select count(distinct concat(str(parent.itemId), ':', b.bomVersion))
                    from BomStructure b
                    join b.parentItem parent
                    join b.childItem child
                    where (:parentKeyword is null
                        or lower(parent.itemCode) like lower(concat('%', :parentKeyword, '%'))
                        or lower(parent.itemName) like lower(concat('%', :parentKeyword, '%'))
                        or str(parent.itemId) = :parentKeyword)
                      and (:childKeyword is null
                        or lower(child.itemCode) like lower(concat('%', :childKeyword, '%'))
                        or lower(child.itemName) like lower(concat('%', :childKeyword, '%'))
                        or str(child.itemId) = :childKeyword)
                      and (:bomVersion is null or b.bomVersion = :bomVersion)
                    """
    )
    Page<BomGroupResponse> searchBomGroups(
            @Param("parentKeyword") String parentKeyword,
            @Param("childKeyword") String childKeyword,
            @Param("bomVersion") String bomVersion,
            @Param("activeStatus") BomStructure.BomStatus activeStatus,
            Pageable pageable
    );
}

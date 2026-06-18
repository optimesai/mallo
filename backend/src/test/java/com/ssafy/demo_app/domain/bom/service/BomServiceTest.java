package com.ssafy.demo_app.domain.bom.service;

import com.ssafy.demo_app.api.bom.dto.BomBulkLineRequest;
import com.ssafy.demo_app.api.bom.dto.BomBulkRequest;
import com.ssafy.demo_app.api.bom.dto.BomGroupResponse;
import com.ssafy.demo_app.api.bom.dto.BomRequest;
import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.bom.dto.BomStatusUpdateRequest;
import com.ssafy.demo_app.api.bom.dto.BomTreeResponse;
import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BomServiceTest {

    @Autowired
    private BomService bomService;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("BOM 등록 성공 - 부모/자식/소요량/버전을 저장한다")
    void createBom_success() {
        ItemMaster parentItem = createItem("BOM-FG-CREATE", "BOM등록 완제품", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("BOM-RM-CREATE", "BOM등록 원자재", ItemMaster.ItemType.RAW);

        BomResponse response = bomService.createBom(new BomRequest(
                parentItem.getItemId(),
                childItem.getItemId(),
                2,
                "v1.0"
        ));

        assertThat(response.getParentItemCode()).isEqualTo("BOM-FG-CREATE");
        assertThat(response.getChildItemCode()).isEqualTo("BOM-RM-CREATE");
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getBomVersion()).isEqualTo("v1.0");
        assertThat(response.getBomStatus()).isEqualTo(BomStructure.BomStatus.ACTIVE.name());
    }

    @Test
    @DisplayName("BOM 일괄 등록 성공 - 하나의 상위 품목과 버전에 여러 구성 품목을 저장한다")
    void createBoms_success() {
        ItemMaster parentItem = createItem("BOM-FG-BULK", "BOM일괄 완제품", ItemMaster.ItemType.FG);
        ItemMaster firstChildItem = createItem("BOM-RM-BULK-A", "BOM일괄 원자재 A", ItemMaster.ItemType.RAW);
        ItemMaster secondChildItem = createItem("BOM-RM-BULK-B", "BOM일괄 원자재 B", ItemMaster.ItemType.RAW);
        BomBulkRequest request = createBulkRequest(
                parentItem.getItemId(),
                "v1.0",
                List.of(
                        createBulkLine(firstChildItem.getItemId(), 2),
                        createBulkLine(secondChildItem.getItemId(), 3)
                )
        );

        List<BomResponse> responses = bomService.createBoms(request);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(BomResponse::getChildItemCode)
                .containsExactlyInAnyOrder("BOM-RM-BULK-A", "BOM-RM-BULK-B");
        assertThat(responses).allSatisfy(response -> assertThat(response.getBomVersion()).isEqualTo("v1.0"));
    }

    @Test
    @DisplayName("BOM 일괄 등록 실패 - 요청 내 구성 품목 중복은 허용하지 않는다")
    void createBoms_duplicateLine() {
        ItemMaster parentItem = createItem("BOM-FG-BULK-DUP", "BOM일괄중복 완제품", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("BOM-RM-BULK-DUP", "BOM일괄중복 원자재", ItemMaster.ItemType.RAW);
        BomBulkRequest request = createBulkRequest(
                parentItem.getItemId(),
                "v1.0",
                List.of(
                        createBulkLine(childItem.getItemId(), 2),
                        createBulkLine(childItem.getItemId(), 3)
                )
        );

        assertThatThrownBy(() -> bomService.createBoms(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_DUPLICATE);
    }

    @Test
    @DisplayName("BOM 그룹 목록 조회 성공 - 상위 품목과 버전 기준으로 10개 단위 페이지를 반환한다")
    void getBomGroups_pagination() {
        ItemMaster childItem = createItem("BOM-RM-PAGE", "BOM페이지 원자재", ItemMaster.ItemType.RAW);
        for (int i = 0; i < 11; i++) {
            ItemMaster parentItem = createItem("BOM-FG-PAGE-" + i, "BOM페이지 완제품" + i, ItemMaster.ItemType.FG);
            createBom(parentItem, childItem, 1, "v1.0");
        }

        var page = bomService.getBomGroups(PageRequest.of(0, 10), "BOM-FG-PAGE", null, null);

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(BomGroupResponse::getChildCount)
                .containsOnly(1);
    }

    @Test
    @DisplayName("BOM 그룹 상세 조회 성공 - 상위 품목 ID와 버전을 정확히 일치시킨다")
    void getBomGroup_exactParentItemId() {
        ItemMaster targetParentItem = createItem("BOM-FG-DETAIL", "BOM상세 완제품", ItemMaster.ItemType.FG);
        em.flush();
        ItemMaster similarParentItem = createItem(
                "BOM-FG-DETAIL-" + targetParentItem.getItemId(),
                "BOM상세 유사 완제품",
                ItemMaster.ItemType.FG
        );
        ItemMaster firstChildItem = createItem("BOM-RM-DETAIL-A", "BOM상세 원자재 A", ItemMaster.ItemType.RAW);
        ItemMaster secondChildItem = createItem("BOM-RM-DETAIL-B", "BOM상세 원자재 B", ItemMaster.ItemType.RAW);
        createBom(targetParentItem, firstChildItem, 1, "v1.0");
        createBom(similarParentItem, secondChildItem, 1, "v1.0");

        List<BomResponse> responses = bomService.getBomGroup(targetParentItem.getItemId(), "v1.0");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getParentItemId()).isEqualTo(targetParentItem.getItemId());
        assertThat(responses.get(0).getChildItemCode()).isEqualTo("BOM-RM-DETAIL-A");
    }

    @Test
    @DisplayName("BOM 등록 실패 - 동일 부모/자식/버전 중복은 허용하지 않는다")
    void createBom_duplicate() {
        ItemMaster parentItem = createItem("BOM-FG-DUP", "BOM중복 완제품", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("BOM-RM-DUP", "BOM중복 원자재", ItemMaster.ItemType.RAW);
        createBom(parentItem, childItem, 1, "v1.0");

        assertThatThrownBy(() -> bomService.createBom(new BomRequest(
                parentItem.getItemId(),
                childItem.getItemId(),
                1,
                "v1.0"
        )))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_DUPLICATE);
    }

    @Test
    @DisplayName("BOM 등록 실패 - 순환 참조는 허용하지 않는다")
    void createBom_cycle() {
        ItemMaster firstHalf = createItem("BOM-HALF-CYCLE-A", "순환 반제품 A", ItemMaster.ItemType.HALF);
        ItemMaster secondHalf = createItem("BOM-HALF-CYCLE-B", "순환 반제품 B", ItemMaster.ItemType.HALF);
        createBom(firstHalf, secondHalf, 1, "v1.0");

        assertThatThrownBy(() -> bomService.createBom(new BomRequest(
                secondHalf.getItemId(),
                firstHalf.getItemId(),
                1,
                "v1.0"
        )))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_CYCLE_DETECTED);
    }

    @Test
    @DisplayName("BOM 상태 변경 성공 - 비활성 BOM은 생산용 소요량 계산에서 제외된다")
    void updateBomStatus_inactive() {
        ItemMaster parentItem = createItem("BOM-FG-STATUS", "상태 완제품", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("BOM-RM-STATUS", "상태 원자재", ItemMaster.ItemType.RAW);
        BomStructure bom = createBom(parentItem, childItem, 1, "v1.0");
        BomStatusUpdateRequest request = new BomStatusUpdateRequest();
        request.setBomStatus(BomStructure.BomStatus.INACTIVE);

        BomResponse response = bomService.updateBomStatus(bom.getBomId(), request);

        assertThat(response.getBomStatus()).isEqualTo(BomStructure.BomStatus.INACTIVE.name());
        assertThatThrownBy(() -> bomService.calculateMaterialRequirements(parentItem, "v1.0", 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_NOT_FOUND);
    }

    @Test
    @DisplayName("BOM 정전개 조회 성공 - 활성 BOM 기준 계층 구조를 반환한다")
    void getBomTreesByParentKeyword_success() {
        ItemMaster parentItem = createItem("BOM-FG-TREE", "트리 완제품", ItemMaster.ItemType.FG);
        ItemMaster halfItem = createItem("BOM-HALF-TREE", "트리 반제품", ItemMaster.ItemType.HALF);
        ItemMaster rawItem = createItem("BOM-RM-TREE", "트리 원자재", ItemMaster.ItemType.RAW);
        createBom(parentItem, halfItem, 2, "v1.0");
        createBom(halfItem, rawItem, 3, "v1.0");

        List<BomTreeResponse> trees = bomService.getBomTreesByParentKeyword(parentItem.getItemCode(), "v1.0");

        assertThat(trees).hasSize(1);
        assertThat(trees.get(0).getChildren()).hasSize(1);
        assertThat(trees.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(trees.get(0).getChildren().get(0).getChildren().get(0).getItemCode()).isEqualTo(rawItem.getItemCode());
    }

    private ItemMaster createItem(String code, String name, ItemMaster.ItemType type) {
        ItemMaster item = new ItemMaster();
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemType(type);
        item.setUnit(ItemMaster.Unit.ea);
        item.setSafetyStock(10);
        em.persist(item);
        return item;
    }

    private BomStructure createBom(ItemMaster parentItem, ItemMaster childItem, Integer quantity, String bomVersion) {
        BomStructure bom = new BomStructure();
        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(quantity);
        bom.setBomVersion(bomVersion);
        em.persist(bom);
        return bom;
    }

    private BomBulkRequest createBulkRequest(Integer parentItemId, String bomVersion, List<BomBulkLineRequest> lines) {
        BomBulkRequest request = new BomBulkRequest();
        request.setParentItemId(parentItemId);
        request.setBomVersion(bomVersion);
        request.setLines(new ArrayList<>(lines));
        return request;
    }

    private BomBulkLineRequest createBulkLine(Integer childItemId, Integer quantity) {
        BomBulkLineRequest line = new BomBulkLineRequest();
        line.setChildItemId(childItemId);
        line.setQuantity(quantity);
        return line;
    }
}

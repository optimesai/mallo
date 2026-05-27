package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class WorkOrderServiceTest {

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("재고 목록 조회 API 검증 - 등록된 창고 및 렉 위치별 품목 재고 수량이 정상 조회되는지 확인")
    void getInventories_success() {
        // Given
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("TEST-INV-GET", "테스트품목", ItemMaster.ItemType.RAW);

        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(150);
        em.persist(inventory);

        em.flush();
        em.clear();

        // When
        List<CurrentInventoryResponse> inventories = inventoryService.getInventories();

        // Then
        assertThat(inventories).isNotEmpty();
        CurrentInventoryResponse target = inventories.stream()
                .filter(i -> i.getItemCode().equals("TEST-INV-GET"))
                .findFirst()
                .orElseThrow();
        assertThat(target.getCurrentQty()).isEqualTo(150);
        assertThat(target.getLocationCode()).isEqualTo(location.getLocationCode());
    }

    @Test
    @DisplayName("수불 이력 조회 API 검증 - 변동 이력(수량, 작업 유형 등) 데이터가 올바르게 반환되는지 확인")
    void getTransactionHistories_success() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("TEST-INV-HIST", "테스트품목HIST", ItemMaster.ItemType.RAW);

        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(item);
        history.setLocation(location);
        history.setTransactionType(InventoryTransactionHistory.TransactionType.INBOUND);
        history.setQuantity(300);
        history.setReasonDesc("Test Inbound Audit");
        history.setWorker(worker);
        em.persist(history);

        em.flush();
        em.clear();

        // When
        List<TransactionHistoryResponse> histories = inventoryService.getTransactionHistories();

        // Then
        assertThat(histories).isNotEmpty();
        TransactionHistoryResponse target = histories.stream()
                .filter(h -> h.getItemCode().equals("TEST-INV-HIST"))
                .findFirst()
                .orElseThrow();
        assertThat(target.getQuantity()).isEqualTo(300);
        assertThat(target.getTransactionType()).isEqualTo(InventoryTransactionHistory.TransactionType.INBOUND.name());
    }

    @Test
    @DisplayName("BOM 기반 생산 자재 출고 성공 - 단순 재고 차감 및 수불 이력 생성 검증")
    void issueMaterials_success() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation loc1 = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);

        // 1. parent 품목 및 child 품목 설정
        ItemMaster parentItem = createItem("PARENT-FG", "완제품 스마트 박스", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("CHILD-RM", "원자재 칩셋", ItemMaster.ItemType.RAW);
        
        // 2. BOM 구성 설정 (완제품 1개당 원자재 2개 소요)
        BomStructure bom = new BomStructure();
        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(BigDecimal.valueOf(2.0));
        em.persist(bom);

        // 3. 재고 설정
        CurrentInventory inv1 = new CurrentInventory();
        inv1.setItem(childItem);
        inv1.setLocation(loc1);
        inv1.setCurrentQty(20);
        em.persist(inv1);

        // 4. 작업 지시 생성 (완제품 6개 생산 목표 -> 원자재 12개 필요)
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo("WO-TEST-001");
        workOrder.setItem(parentItem);
        workOrder.setTargetQty(6);
        workOrder.setPlanDate(LocalDate.now());
        workOrder.setStatus(WorkOrder.OrderStatus.READY);
        workOrder.setRouting(routing);
        em.persist(workOrder);

        em.flush();
        em.clear();

        // When
        workOrderService.issueMaterials(workOrder.getOrderId(), worker.getUserId());

        // Then
        // 12개 자재를 차감해야 하므로, 20 - 12 = 8개가 되어야 함
        CurrentInventory updatedInv1 = em.createQuery(
                "select ci from CurrentInventory ci where ci.item = :item and ci.location = :location", CurrentInventory.class)
                .setParameter("item", childItem)
                .setParameter("location", loc1)
                .getSingleResult();
        
        assertThat(updatedInv1.getCurrentQty()).isEqualTo(8);

        // 작업 지시의 상태가 RUN으로 변경되었는지 검증
        WorkOrder updatedWorkOrder = em.find(WorkOrder.class, workOrder.getOrderId());
        assertThat(updatedWorkOrder.getStatus()).isEqualTo(WorkOrder.OrderStatus.RUN);

        // 수불 이력이 생성되었는지 검증
        List<InventoryTransactionHistory> histories = em.createQuery(
                "select h from InventoryTransactionHistory h where h.item.itemCode = 'CHILD-RM'", InventoryTransactionHistory.class)
                .getResultList();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getQuantity()).isEqualTo(12);
    }

    @Test
    @DisplayName("BOM 기반 생산 자재 출고 실패 - 재고 부족 시 INSUFFICIENT_STOCK 예외 발생")
    void issueMaterials_insufficientStock() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation loc = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);

        ItemMaster parentItem = createItem("PARENT-FG2", "완제품 스마트 박스2", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("CHILD-RM2", "원자재 칩셋2", ItemMaster.ItemType.RAW);

        BomStructure bom = new BomStructure();
        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(BigDecimal.valueOf(5.0));
        em.persist(bom);

        // 현재고 3개 설정
        CurrentInventory inv = new CurrentInventory();
        inv.setItem(childItem);
        inv.setLocation(loc);
        inv.setCurrentQty(3);
        em.persist(inv);

        // 1개 목표 생산 지시 -> 5개 자재 소요되므로 2개 부족
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo("WO-TEST-002");
        workOrder.setItem(parentItem);
        workOrder.setTargetQty(1);
        workOrder.setPlanDate(LocalDate.now());
        workOrder.setStatus(WorkOrder.OrderStatus.READY);
        workOrder.setRouting(routing);
        em.persist(workOrder);

        em.flush();
        em.clear();

        // When & Then
        assertThatThrownBy(() -> workOrderService.issueMaterials(workOrder.getOrderId(), worker.getUserId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
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
}

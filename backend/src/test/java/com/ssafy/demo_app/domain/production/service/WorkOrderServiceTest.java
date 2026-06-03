package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionCreateRequest;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderCloseRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderCreateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderStatusUpdateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderUpdateRequest;
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
    private ProductionExecutionService productionExecutionService;

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
    @DisplayName("작업 지시 생성 성공 - 작업 지시 번호는 계획일 기준으로 자동 생성된다")
    void createWorkOrder_generatesOrderNo() {
        // Given
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-AUTO", "자동번호 완제품", ItemMaster.ItemType.FG);
        LocalDate planDate = LocalDate.of(2026, 6, 3);

        WorkOrderResponse response = workOrderService.createWorkOrder(
                new WorkOrderCreateRequest(item.getItemCode(), routing.getRoutingId(), 10, planDate)
        );

        assertThat(response.getOrderNo()).startsWith("WO-20260603-");
        assertThat(response.getStatus()).isEqualTo(WorkOrder.OrderStatus.READY.name());
        assertThat(response.getTargetQty()).isEqualTo(10);
    }

    @Test
    @DisplayName("작업 지시 수정 실패 - READY 상태가 아니면 수정할 수 없다")
    void updateWorkOrder_onlyReadyAllowed() {
        // Given
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-RUN", "진행중 완제품", ItemMaster.ItemType.FG);
        WorkOrder workOrder = createWorkOrder("WO-READY-LIMIT", item, routing, 10, WorkOrder.OrderStatus.RUN);
        em.flush();
        em.clear();

        // When & Then
        assertThatThrownBy(() -> workOrderService.updateWorkOrder(
                String.valueOf(workOrder.getOrderId()),
                new WorkOrderUpdateRequest(item.getItemCode(), routing.getRoutingId(), 20, LocalDate.now())
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_ORDER_STATUS_INVALID);
    }

    @Test
    @DisplayName("생산 실적 등록 및 목표 미달 마감 검증 - 허용값이 true일 때만 목표 미달 마감 가능")
    void closeWorkOrder_underTargetRequiresRequestFlag() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-CLOSE", "미달마감 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-CLOSE", "미달마감 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, BigDecimal.valueOf(2));
        createInventory(material, location, 100);
        WorkOrder workOrder = createWorkOrder("WO-CLOSE-001", item, routing, 10, WorkOrder.OrderStatus.RUN);
        em.flush();
        em.clear();

        ProductionExecutionResponse execution = productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), routing.getRoutingId(), 8, 1, 120)
        );
        assertThat(execution.getRoutingId()).isEqualTo(routing.getRoutingId());
        assertThat(execution.getOperationName()).isEqualTo(routing.getOperationName());

        assertThatThrownBy(() -> workOrderService.closeWorkOrder(
                workOrder.getOrderNo(),
                new WorkOrderCloseRequest(false)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_ORDER_CLOSE_UNDER_TARGET);

        WorkOrderResponse closed = workOrderService.closeWorkOrder(
                workOrder.getOrderNo(),
                new WorkOrderCloseRequest(true)
        );

        assertThat(closed.getStatus()).isEqualTo(WorkOrder.OrderStatus.CLOSE.name());
        assertThat(closed.getTotalGoodQty()).isEqualTo(8);
    }

    @Test
    @DisplayName("작업 지시 상태 변경 성공 - READY 상태는 RUN 상태로 변경할 수 있다")
    void updateStatus_readyToRun_success() {
        // Given
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-STATUS", "상태변경 완제품", ItemMaster.ItemType.FG);
        WorkOrder workOrder = createWorkOrder("WO-STATUS-001", item, routing, 10, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        // When
        WorkOrderResponse response = workOrderService.updateStatus(
                workOrder.getOrderNo(),
                new WorkOrderStatusUpdateRequest(WorkOrder.OrderStatus.RUN)
        );

        // Then
        assertThat(response.getStatus()).isEqualTo(WorkOrder.OrderStatus.RUN.name());
    }

    @Test
    @DisplayName("생산 실적 등록 성공 - 실적 수량 기준 BOM 자재가 자동 차감된다")
    void createExecution_autoIssuesMaterialsByExecutedQty() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-AUTO-ISSUE", "자동불출 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-AUTO-ISSUE", "자동불출 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, BigDecimal.valueOf(2));
        createInventory(material, location, 100);
        WorkOrder workOrder = createWorkOrder("WO-AUTO-ISSUE-001", item, routing, 10, WorkOrder.OrderStatus.RUN);
        em.flush();
        em.clear();

        // When
        productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), routing.getRoutingId(), 4, 1, 60)
        );

        // Then
        CurrentInventory inventory = em.createQuery(
                        "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode", CurrentInventory.class)
                .setParameter("itemCode", "WO-RM-AUTO-ISSUE")
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(90);
    }

    @Test
    @DisplayName("생산 실적 등록 실패 - RUN 상태 작업 지시에만 실적을 등록할 수 있다")
    void createExecution_requiresRunStatus() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-EXEC", "실적상태 완제품", ItemMaster.ItemType.FG);
        WorkOrder workOrder = createWorkOrder("WO-EXEC-001", item, routing, 10, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        // When & Then
        assertThatThrownBy(() -> productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), routing.getRoutingId(), 1, 0, 30)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_ORDER_STATUS_INVALID);
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
        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());

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
        assertThatThrownBy(() -> workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId()))
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

    private void createBom(ItemMaster parentItem, ItemMaster childItem, BigDecimal quantity) {
        BomStructure bom = new BomStructure();
        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(quantity);
        em.persist(bom);
    }

    private void createInventory(ItemMaster item, WarehouseLocation location, Integer currentQty) {
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(currentQty);
        em.persist(inventory);
    }

    private WorkOrder createWorkOrder(
            String orderNo,
            ItemMaster item,
            FactoryRouting routing,
            Integer targetQty,
            WorkOrder.OrderStatus status
    ) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(orderNo);
        workOrder.setItem(item);
        workOrder.setRouting(routing);
        workOrder.setTargetQty(targetQty);
        workOrder.setPlanDate(LocalDate.now());
        workOrder.setStatus(status);
        em.persist(workOrder);
        return workOrder;
    }
}

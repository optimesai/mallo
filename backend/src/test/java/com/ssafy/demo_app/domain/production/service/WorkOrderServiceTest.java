package com.ssafy.demo_app.domain.production.service;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;

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

import com.ssafy.demo_app.global.response.PageResponse;

import org.springframework.data.domain.Pageable;

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
        PageResponse<CurrentInventoryResponse> inventories = inventoryService.getInventories(Pageable.unpaged(), null);

        // Then
        assertThat(inventories.getContent()).isNotEmpty();
        CurrentInventoryResponse target = inventories.getContent().stream()
                .filter(i -> i.getItemCode().equals("TEST-INV-GET"))
                .findFirst()
                .orElseThrow();
        assertThat(target.getCurrentQty()).isEqualTo(150);
        assertThat(target.getLocationCode()).isEqualTo(location.getLocationCode());
    }

    @Test
    @DisplayName("재고 목록 조회 API 검증 - 현재고 행이 없는 품목도 0개 재고로 조회된다")
    void getInventories_includesItemWithoutInventory() {
        // Given
        ItemMaster item = createItem("TEST-INV-ZERO", "재고없는품목", ItemMaster.ItemType.RAW);

        em.flush();
        em.clear();

        // When
        PageResponse<CurrentInventoryResponse> inventories = inventoryService.getInventories(Pageable.unpaged(), null);

        // Then
        CurrentInventoryResponse target = inventories.getContent().stream()
                .filter(i -> i.getItemCode().equals(item.getItemCode()))
                .findFirst()
                .orElseThrow();
        assertThat(target.getInventoryId()).isNull();
        assertThat(target.getCurrentQty()).isZero();
        assertThat(target.getLocationCode()).isNull();
        assertThat(target.getWarehouseName()).isNull();
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
        history.setTransactionType(TransactionType.INBOUND);
        history.setQuantity(300);
        history.setReasonDesc("Test Inbound Audit");
        history.setWorker(worker);
        em.persist(history);

        em.flush();
        em.clear();

        // When
        PageResponse<TransactionHistoryResponse> histories = inventoryService.getTransactionHistories(Pageable.unpaged(), null, null, null);

        // Then
        assertThat(histories.getContent()).isNotEmpty();
        TransactionHistoryResponse target = histories.getContent().stream()
                .filter(h -> h.getItemCode().equals("TEST-INV-HIST"))
                .findFirst()
                .orElseThrow();
        assertThat(target.getQuantity()).isEqualTo(300);
        assertThat(target.getTransactionType()).isEqualTo(TransactionType.INBOUND.name());
    }

    @Test
    @DisplayName("작업 지시 생성 성공 - 작업 지시 번호는 계획일 기준으로 자동 생성된다")
    void createWorkOrder_generatesOrderNo() {
        // Given
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-AUTO", "자동번호 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-AUTO", "자동번호 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, 1);
        LocalDate planDate = LocalDate.of(2026, 6, 3);

        WorkOrderResponse response = workOrderService.createWorkOrder(
                new WorkOrderCreateRequest(item.getItemCode(), routing.getRoutingId(), 10, "v1.0", planDate)
        );

        assertThat(response.getOrderNo()).startsWith("WO-20260603-");
        assertThat(response.getStatus()).isEqualTo(WorkOrder.OrderStatus.READY.name());
        assertThat(response.getTargetQty()).isEqualTo(10);
    }

    @Test
    @DisplayName("작업 지시 생성 실패 - 비활성 라우팅은 신규 작업 지시에 사용할 수 없다")
    void createWorkOrder_inactiveRouting() {
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        routing.setRoutingStatus(FactoryRouting.RoutingStatus.INACTIVE);
        ItemMaster item = createItem("WO-FG-INACTIVE", "비활성라우팅 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-INACTIVE", "비활성라우팅 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, 1);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> workOrderService.createWorkOrder(
                new WorkOrderCreateRequest(item.getItemCode(), routing.getRoutingId(), 10, "v1.0", LocalDate.now())
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTING_INACTIVE);
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
                new WorkOrderUpdateRequest(item.getItemCode(), routing.getRoutingId(), 20, "v1.0", LocalDate.now())
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORK_ORDER_STATUS_INVALID);
    }

    @Test
    @DisplayName("생산 실적 등록 및 목표 미달 마감 검증 - 허용값이 true일 때만 목표 미달 마감 가능")
    void closeWorkOrder_underTargetRequiresRequestFlag() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        List<FactoryRouting> lineRoutings = em.createQuery(
                        "select r from FactoryRouting r where r.factoryName = :factoryName and r.lineName = :lineName order by r.operationSeq asc",
                        FactoryRouting.class)
                .setParameter("factoryName", "창원제1공장")
                .setParameter("lineName", "A라인")
                .getResultList();
        FactoryRouting firstRouting = lineRoutings.get(0);
        FactoryRouting secondRouting = lineRoutings.get(1);
        FactoryRouting lastRouting = lineRoutings.get(2);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        location.setProductionReceiptDefault(true);
        ItemMaster item = createItem("WO-FG-CLOSE", "미달마감 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-CLOSE", "미달마감 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, 2);
        createInventory(material, location, 100);
        WorkOrder workOrder = createWorkOrder("WO-CLOSE-001", item, firstRouting, 10, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());

        productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), firstRouting.getRoutingId(), 9, 0, 60)
        );
        productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), secondRouting.getRoutingId(), 9, 0, 60)
        );
        ProductionExecutionCreateRequest request = new ProductionExecutionCreateRequest(workOrder.getOrderNo(), lastRouting.getRoutingId(), 8, 1, 120);
        request.setDefectReason("테스트 불량");
        ProductionExecutionResponse execution = productionExecutionService.createExecution(worker.getUserId(), request);
        assertThat(execution.getRoutingId()).isEqualTo(lastRouting.getRoutingId());
        assertThat(execution.getOperationName()).isEqualTo(lastRouting.getOperationName());

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
    @DisplayName("작업 지시 검색 성공 - 일반 키워드로 작업 지시 번호를 조회할 수 있다")
    void getWorkOrders_keywordSearchesOrderNo() {
        // Given
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-SEARCH", "검색검증 완제품", ItemMaster.ItemType.FG);
        WorkOrder workOrder = createWorkOrder("WO-SEARCH-001", item, routing, 10, WorkOrder.OrderStatus.RUN);
        em.flush();
        em.clear();

        // When
        PageResponse<WorkOrderResponse> response = workOrderService.getWorkOrders(
                Pageable.unpaged(),
                WorkOrder.OrderStatus.RUN,
                null,
                null,
                null,
                workOrder.getOrderNo(),
                null,
                null,
                null,
                null
        );

        // Then
        assertThat(response.getContent())
                .extracting(WorkOrderResponse::getOrderNo)
                .contains(workOrder.getOrderNo());
    }

    @Test
    @DisplayName("생산 실적 등록 성공 - 자재 출고 후 실적 등록 시 BOM 자재를 추가 차감하지 않는다")
    void createExecution_autoIssuesMaterialsByExecutedQty() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = createRouting("테스트공장", "추가차감방지라인", 1, "단일 공정");
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        location.setProductionReceiptDefault(true);
        ItemMaster item = createItem("WO-FG-AUTO-ISSUE", "자동불출 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-AUTO-ISSUE", "자동불출 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, 2);
        createInventory(material, location, 100);
        WorkOrder workOrder = createWorkOrder("WO-AUTO-ISSUE-001", item, routing, 10, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        // When
        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());
        ProductionExecutionCreateRequest request = new ProductionExecutionCreateRequest(workOrder.getOrderNo(), routing.getRoutingId(), 4, 1, 60);
        request.setDefectReason("테스트 불량");
        productionExecutionService.createExecution(worker.getUserId(), request);

        // Then
        CurrentInventory inventory = em.createQuery(
                "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode", CurrentInventory.class)
                .setParameter("itemCode", "WO-RM-AUTO-ISSUE")
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(80);
    }

    @Test
    @DisplayName("생산 실적 등록 성공 - 기본 생산 입고 로케이션이 없으면 첫 로케이션을 사용한다")
    void createExecution_usesFirstLocationWhenDefaultMissing() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = createRouting("테스트공장", "기본입고대체라인", 1, "단일 공정");
        List<WarehouseLocation> locations = em.createQuery("select l from WarehouseLocation l order by l.locationId asc", WarehouseLocation.class).getResultList();
        locations.forEach(location -> location.setProductionReceiptDefault(false));
        WarehouseLocation fallbackLocation = locations.get(0);
        ItemMaster item = createItem("WO-FG-DEFAULT-FALLBACK", "기본입고대체 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-DEFAULT-FALLBACK", "기본입고대체 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, 1);
        createInventory(material, fallbackLocation, 100);
        WorkOrder workOrder = createWorkOrder("WO-DEFAULT-FALLBACK-001", item, routing, 10, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        // When
        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());
        productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), routing.getRoutingId(), 3, 0, 30)
        );

        // Then
        CurrentInventory inventory = em.createQuery(
                        "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode and ci.location.locationId = :locationId",
                        CurrentInventory.class)
                .setParameter("itemCode", "WO-FG-DEFAULT-FALLBACK")
                .setParameter("locationId", fallbackLocation.getLocationId())
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(3);
    }

    @Test
    @DisplayName("생산 실적 등록 성공 - 지정 로케이션에 lot 재고가 있어도 lot 없는 생산 입고 재고를 생성한다")
    void createExecution_receivesToSpecifiedLocationWithLotInventories() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = createRouting("테스트공장", "로트입고라인", 1, "단일 공정");
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("WO-FG-LOT-RECEIPT", "Lot입고 완제품", ItemMaster.ItemType.FG);
        ItemMaster material = createItem("WO-RM-LOT-RECEIPT", "Lot입고 원자재", ItemMaster.ItemType.RAW);
        createBom(item, material, 1);
        createInventory(material, location, 100);
        createInventory(item, location, 4, "LOT-A");
        createInventory(item, location, 6, "LOT-B");
        WorkOrder workOrder = createWorkOrder("WO-LOT-RECEIPT-001", item, routing, 10, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        // When
        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());
        ProductionExecutionCreateRequest request = new ProductionExecutionCreateRequest(workOrder.getOrderNo(), routing.getRoutingId(), 5, 0, 30);
        request.setReceiptLocationCode(location.getLocationCode());
        productionExecutionService.createExecution(worker.getUserId(), request);

        // Then
        CurrentInventory inventory = em.createQuery(
                        "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode and ci.location.locationId = :locationId and ci.lotNumber is null",
                        CurrentInventory.class)
                .setParameter("itemCode", "WO-FG-LOT-RECEIPT")
                .setParameter("locationId", location.getLocationId())
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(5);
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
    @DisplayName("생산 실적 등록 실패 - 작업 지시와 다른 공장/라인 라우팅은 사용할 수 없다")
    void createExecution_rejectsDifferentLineRouting() {
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        List<FactoryRouting> routings = em.createQuery("select r from FactoryRouting r order by r.routingId asc", FactoryRouting.class).getResultList();
        FactoryRouting orderRouting = routings.get(0);
        FactoryRouting otherLineRouting = routings.stream()
                .filter(routing -> !routing.getFactoryName().equals(orderRouting.getFactoryName())
                        || !routing.getLineName().equals(orderRouting.getLineName()))
                .findFirst()
                .orElseThrow();
        ItemMaster item = createItem("WO-FG-ROUTE-MISMATCH", "라우팅불일치 완제품", ItemMaster.ItemType.FG);
        WorkOrder workOrder = createWorkOrder("WO-ROUTE-MISMATCH-001", item, orderRouting, 10, WorkOrder.OrderStatus.RUN);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> productionExecutionService.createExecution(
                worker.getUserId(),
                new ProductionExecutionCreateRequest(workOrder.getOrderNo(), otherLineRouting.getRoutingId(), 1, 0, 30)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCTION_EXECUTION_ROUTING_MISMATCH);
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
        bom.setQuantity(2);
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
    @DisplayName("BOM 기반 생산 자재 출고 성공 - 작업지시에 지정된 BOM 버전만 사용한다")
    void issueMaterials_usesWorkOrderBomVersion() {
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster parentItem = createItem("WO-FG-BOM-VERSION", "버전 완제품", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("WO-RM-BOM-VERSION", "버전 원자재", ItemMaster.ItemType.RAW);
        createBom(parentItem, childItem, 1, "v1.0");
        createBom(parentItem, childItem, 3, "v2.0");
        createInventory(childItem, location, 20);
        WorkOrder workOrder = createWorkOrder("WO-BOM-VERSION-001", parentItem, routing, 2, WorkOrder.OrderStatus.READY, "v2.0");
        em.flush();
        em.clear();

        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());

        CurrentInventory inventory = em.createQuery(
                        "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode", CurrentInventory.class)
                .setParameter("itemCode", "WO-RM-BOM-VERSION")
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(14);
    }

    @Test
    @DisplayName("BOM 기반 생산 자재 출고 성공 - 반제품 BOM은 하위 원자재까지 전개한다")
    void issueMaterials_expandsHalfItemBom() {
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster finishedItem = createItem("WO-FG-MULTI", "다단계 완제품", ItemMaster.ItemType.FG);
        ItemMaster halfItem = createItem("WO-HALF-MULTI", "다단계 반제품", ItemMaster.ItemType.HALF);
        ItemMaster rawItem = createItem("WO-RM-MULTI", "다단계 원자재", ItemMaster.ItemType.RAW);
        createBom(finishedItem, halfItem, 2);
        createBom(halfItem, rawItem, 3);
        createInventory(rawItem, location, 20);
        WorkOrder workOrder = createWorkOrder("WO-MULTI-001", finishedItem, routing, 2, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());

        CurrentInventory inventory = em.createQuery(
                        "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode", CurrentInventory.class)
                .setParameter("itemCode", "WO-RM-MULTI")
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(8);
    }

    @Test
    @DisplayName("BOM 기반 생산 자재 출고 성공 - 정수 BOM 소요량 기준으로 차감한다")
    void issueMaterials_usesIntegerBomQuantity() {
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        FactoryRouting routing = em.createQuery("select r from FactoryRouting r", FactoryRouting.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster parentItem = createItem("WO-FG-INTEGER", "정수 완제품", ItemMaster.ItemType.FG);
        ItemMaster childItem = createItem("WO-RM-INTEGER", "정수 원자재", ItemMaster.ItemType.RAW);
        createBom(parentItem, childItem, 2);
        createInventory(childItem, location, 10);
        WorkOrder workOrder = createWorkOrder("WO-INTEGER-001", parentItem, routing, 3, WorkOrder.OrderStatus.READY);
        em.flush();
        em.clear();

        workOrderService.issueMaterials(workOrder.getOrderNo(), worker.getUserId());

        CurrentInventory inventory = em.createQuery(
                        "select ci from CurrentInventory ci where ci.item.itemCode = :itemCode", CurrentInventory.class)
                .setParameter("itemCode", "WO-RM-INTEGER")
                .getSingleResult();
        assertThat(inventory.getCurrentQty()).isEqualTo(4);
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
        bom.setQuantity(5);
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

    private void createBom(ItemMaster parentItem, ItemMaster childItem, Integer quantity) {
        createBom(parentItem, childItem, quantity, "v1.0");
    }

    private void createBom(ItemMaster parentItem, ItemMaster childItem, Integer quantity, String bomVersion) {
        BomStructure bom = new BomStructure();
        bom.setParentItem(parentItem);
        bom.setChildItem(childItem);
        bom.setQuantity(quantity);
        bom.setBomVersion(bomVersion);
        em.persist(bom);
    }

    private void createInventory(ItemMaster item, WarehouseLocation location, Integer currentQty) {
        createInventory(item, location, currentQty, null);
    }

    private void createInventory(ItemMaster item, WarehouseLocation location, Integer currentQty, String lotNumber) {
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setLotNumber(lotNumber);
        inventory.setCurrentQty(currentQty);
        em.persist(inventory);
    }

    private FactoryRouting createRouting(String factoryName, String lineName, Integer operationSeq, String operationName) {
        FactoryRouting routing = new FactoryRouting();
        routing.setFactoryName(factoryName);
        routing.setLineName(lineName);
        routing.setOperationSeq(operationSeq);
        routing.setOperationName(operationName);
        routing.setRoutingStatus(FactoryRouting.RoutingStatus.ACTIVE);
        em.persist(routing);
        return routing;
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
        workOrder.setBomVersion("v1.0");
        workOrder.setPlanDate(LocalDate.now());
        workOrder.setStatus(status);
        em.persist(workOrder);
        return workOrder;
    }

    private WorkOrder createWorkOrder(
            String orderNo,
            ItemMaster item,
            FactoryRouting routing,
            Integer targetQty,
            WorkOrder.OrderStatus status,
            String bomVersion
    ) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(orderNo);
        workOrder.setItem(item);
        workOrder.setRouting(routing);
        workOrder.setTargetQty(targetQty);
        workOrder.setBomVersion(bomVersion);
        workOrder.setPlanDate(LocalDate.now());
        workOrder.setStatus(status);
        em.persist(workOrder);
        return workOrder;
    }
}

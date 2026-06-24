package com.ssafy.demo_app.domain.inventory.repository;

import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventoryTransactionHistoryRepositoryTest {

    @Autowired
    private InventoryTransactionHistoryRepository inventoryTransactionHistoryRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("대시보드 생산 집계 - 마지막 공정의 생산 입고 수량만 공장/라인 기준으로 집계한다")
    void aggregateProductionReceiptByLine_countsOnlyProductionReceipt() {
        FactoryRouting firstRouting = createRouting("집계테스트공장", "A라인", 1, "1공정");
        FactoryRouting lastRouting = createRouting("집계테스트공장", "A라인", 2, "2공정");
        ItemMaster item = createItem("DASH-FG-LINE", "대시보드 완제품");
        WarehouseLocation location = createLocation("DASH-RECEIPT", "생산입고창고");
        WorkOrder workOrder = createWorkOrder("WO-DASH-RECEIPT", item, firstRouting);

        createExecution(workOrder, firstRouting, 10, 0);
        ProductionExecution lastExecution = createExecution(workOrder, lastRouting, 8, 1);
        InventoryTransactionHistory receipt = createHistory(
                item,
                location,
                TransactionType.PRODUCTION_RECEIPT,
                8,
                workOrder,
                lastExecution,
                null
        );
        createHistory(item, location, TransactionType.PRODUCTION_RECEIPT_CANCEL, -4, workOrder, null, receipt);

        em.flush();
        em.clear();

        List<Object[]> rows = inventoryTransactionHistoryRepository.aggregateProductionReceiptByLine(LocalDateTime.now().minusDays(1));
        long total = inventoryTransactionHistoryRepository.sumProductionReceiptQty(LocalDateTime.now().minusDays(1));

        assertThat(rows)
                .filteredOn(row -> "집계테스트공장/A라인".equals(row[0]))
                .isEmpty();
        assertThat(total).isZero();
    }

    @Test
    @DisplayName("대시보드 생산 집계 - 취소되지 않은 생산 입고 수량만 전체 생산량에 반영한다")
    void sumProductionReceiptQty_countsUncancelledReceipt() {
        FactoryRouting firstRouting = createRouting("집계테스트공장", "B라인", 1, "1공정");
        FactoryRouting lastRouting = createRouting("집계테스트공장", "B라인", 2, "2공정");
        ItemMaster item = createItem("DASH-FG-TOTAL", "대시보드 총량 완제품");
        WarehouseLocation location = createLocation("DASH-RECEIPT-TOTAL", "생산입고창고2");
        WorkOrder workOrder = createWorkOrder("WO-DASH-TOTAL", item, firstRouting);

        createExecution(workOrder, firstRouting, 11, 0);
        ProductionExecution lastExecution = createExecution(workOrder, lastRouting, 5, 0);
        createHistory(item, location, TransactionType.PRODUCTION_RECEIPT, 5, workOrder, lastExecution, null);

        em.flush();
        em.clear();

        List<Object[]> rows = inventoryTransactionHistoryRepository.aggregateProductionReceiptByLine(LocalDateTime.now().minusDays(1));
        long total = inventoryTransactionHistoryRepository.sumProductionReceiptQty(LocalDateTime.now().minusDays(1));

        Object[] target = rows.stream()
                .filter(row -> "집계테스트공장/B라인".equals(row[0]))
                .findFirst()
                .orElseThrow();
        assertThat(toLong(target[1])).isEqualTo(5L);
        assertThat(toLong(target[2])).isZero();
        assertThat(total).isGreaterThanOrEqualTo(5L);
    }

    private ItemMaster createItem(String code, String name) {
        ItemMaster item = new ItemMaster();
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemType(ItemMaster.ItemType.FG);
        item.setUnit(ItemMaster.Unit.ea);
        item.setSafetyStock(0);
        em.persist(item);
        return item;
    }

    private WarehouseLocation createLocation(String code, String warehouseName) {
        WarehouseLocation location = new WarehouseLocation();
        location.setLocationCode(code);
        location.setWarehouseName(warehouseName);
        location.setRackRow("A");
        location.setRackColumn("1");
        location.setProductionReceiptDefault(false);
        em.persist(location);
        return location;
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

    private WorkOrder createWorkOrder(String orderNo, ItemMaster item, FactoryRouting routing) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(orderNo);
        workOrder.setItem(item);
        workOrder.setRouting(routing);
        workOrder.setTargetQty(10);
        workOrder.setBomVersion("v1.0");
        workOrder.setPlanDate(LocalDate.now());
        workOrder.setStatus(WorkOrder.OrderStatus.RUN);
        em.persist(workOrder);
        return workOrder;
    }

    private ProductionExecution createExecution(WorkOrder workOrder, FactoryRouting routing, Integer goodQty, Integer defectQty) {
        ProductionExecution execution = new ProductionExecution();
        execution.setOrder(workOrder);
        execution.setRouting(routing);
        execution.setGoodQty(goodQty);
        execution.setDefectQty(defectQty);
        execution.setManHoursMinutes(30);
        em.persist(execution);
        return execution;
    }

    private InventoryTransactionHistory createHistory(
            ItemMaster item,
            WarehouseLocation location,
            TransactionType transactionType,
            Integer quantity,
            WorkOrder workOrder,
            ProductionExecution productionExecution,
            InventoryTransactionHistory originalTransaction
    ) {
        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(item);
        history.setLocation(location);
        history.setTransactionType(transactionType);
        history.setQuantity(quantity);
        history.setReasonDesc(transactionType.name());
        history.setWorkOrder(workOrder);
        history.setProductionExecution(productionExecution);
        history.setOriginalTransaction(originalTransaction);
        em.persist(history);
        return history;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}

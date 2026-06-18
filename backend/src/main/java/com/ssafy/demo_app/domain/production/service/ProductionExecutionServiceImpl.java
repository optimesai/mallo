package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionCreateRequest;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.domain.bom.service.BomRequirement;
import com.ssafy.demo_app.domain.bom.service.BomService;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.WarehouseLocationRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.production.repository.ProductionExecutionRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.domain.routing.repository.FactoryRoutingRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionExecutionServiceImpl implements ProductionExecutionService {

    private final ProductionExecutionRepository productionExecutionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final FactoryRoutingRepository factoryRoutingRepository;
    private final BomService bomService;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;

    @Override
    @Transactional
    public ProductionExecutionResponse createExecution(Integer workerId, ProductionExecutionCreateRequest request) {
        WorkOrder workOrder = findWorkOrder(request.getOrderKey());
        if (workOrder.getStatus() != WorkOrder.OrderStatus.RUN) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        if (request.getGoodQty() + request.getDefectQty() <= 0) {
            throw new BusinessException(ErrorCode.PRODUCTION_EXECUTION_INVALID_QUANTITY);
        }
        if (request.getDefectQty() > 0 && normalize(request.getDefectReason()) == null) {
            throw new BusinessException(ErrorCode.PRODUCTION_EXECUTION_INVALID_QUANTITY);
        }

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        FactoryRouting routing = factoryRoutingRepository.findById(request.getRoutingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTING_NOT_FOUND));
        validateRoutingMatchesWorkOrder(workOrder, routing);
        validatePreviousOperations(workOrder, routing, request.getGoodQty() + request.getDefectQty());

        ProductionExecution execution = new ProductionExecution();
        execution.setOrder(workOrder);
        execution.setRouting(routing);
        execution.setGoodQty(request.getGoodQty());
        execution.setDefectQty(request.getDefectQty());
        execution.setDefectType(normalize(request.getDefectType()));
        execution.setDefectReason(normalize(request.getDefectReason()));
        execution.setReworkable(Boolean.TRUE.equals(request.getReworkable()));
        execution.setWorker(worker);
        execution.setManHoursMinutes(request.getManHoursMinutes());

        ProductionExecution saved = productionExecutionRepository.save(execution);
        issueMaterialsForExecution(workOrder, worker, saved, request.getGoodQty() + request.getDefectQty());
        receiveProduction(workOrder, worker, saved, request.getGoodQty(), request.getReceiptLocationCode());
        return ProductionExecutionResponse.from(saved);
    }

    @Override
    public List<ProductionExecutionResponse> getExecutions(String orderKey) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        return productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder).stream()
                .map(ProductionExecutionResponse::from)
                .toList();
    }

    @Override
    public ProductionExecutionResponse getExecution(Integer executionId) {
        return ProductionExecutionResponse.from(findExecution(executionId));
    }

    @Override
    @Transactional
    public void deleteExecution(Integer executionId) {
        ProductionExecution execution = findExecution(executionId);
        if (execution.getOrder().getStatus() == WorkOrder.OrderStatus.CLOSE) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        User worker = execution.getWorker();
        List<InventoryTransactionHistory> histories = transactionHistoryRepository.findByProductionExecutionOrderByTransactionIdAsc(execution);
        for (InventoryTransactionHistory history : histories) {
            if (history.getTransactionType() == TransactionType.PRODUCTION_ISSUE) {
                restoreInventory(
                        history.getItem(),
                        history.getLocation(),
                        history.getQuantity(),
                        worker,
                        execution,
                        TransactionType.PRODUCTION_ISSUE_CANCEL,
                        "Production issue cancel for Execution: " + execution.getExecutionId(),
                        history
                );
            }
            if (history.getTransactionType() == TransactionType.PRODUCTION_RECEIPT) {
                deductInventory(
                        history.getItem(),
                        history.getLocation(),
                        history.getQuantity(),
                        worker,
                        execution,
                        TransactionType.PRODUCTION_RECEIPT_CANCEL,
                        "Production receipt cancel for Execution: " + execution.getExecutionId(),
                        history
                );
            }
        }
        for (InventoryTransactionHistory history : histories) {
            history.setProductionExecution(null);
            transactionHistoryRepository.save(history);
        }
        productionExecutionRepository.delete(execution);
    }

    private WorkOrder findWorkOrder(String orderKey) {
        if (orderKey != null && orderKey.matches("\\d+")) {
            try {
                return workOrderRepository.findByIdForUpdate(Integer.valueOf(orderKey))
                        .orElseThrow(() -> new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND));
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
            }
        }
        return workOrderRepository.findByOrderNoForUpdate(orderKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND));
    }

    private ProductionExecution findExecution(Integer executionId) {
        return productionExecutionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCTION_EXECUTION_NOT_FOUND));
    }

    private void validateRoutingMatchesWorkOrder(WorkOrder workOrder, FactoryRouting routing) {
        FactoryRouting workOrderRouting = workOrder.getRouting();
        if (!workOrderRouting.getFactoryName().equals(routing.getFactoryName())
                || !workOrderRouting.getLineName().equals(routing.getLineName())) {
            throw new BusinessException(ErrorCode.PRODUCTION_EXECUTION_ROUTING_MISMATCH);
        }
    }

    private void validatePreviousOperations(WorkOrder workOrder, FactoryRouting routing, int newExecutedQty) {
        int currentRoutingExecutedQty = productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder).stream()
                .filter(execution -> execution.getRouting() != null)
                .filter(execution -> execution.getRouting().getRoutingId().equals(routing.getRoutingId()))
                .mapToInt(execution -> execution.getGoodQty() + execution.getDefectQty())
                .sum();
        int nextRoutingExecutedQty = currentRoutingExecutedQty + newExecutedQty;

        List<FactoryRouting> previousRoutings = factoryRoutingRepository.findByFactoryNameAndLineNameOrderByOperationSeqAsc(
                        routing.getFactoryName(),
                        routing.getLineName()
                )
                .stream()
                .filter(candidate -> candidate.getOperationSeq() < routing.getOperationSeq())
                .toList();

        for (FactoryRouting previousRouting : previousRoutings) {
            int previousExecutedQty = productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder).stream()
                    .filter(execution -> execution.getRouting() != null)
                    .filter(execution -> execution.getRouting().getRoutingId().equals(previousRouting.getRoutingId()))
                    .mapToInt(execution -> execution.getGoodQty() + execution.getDefectQty())
                    .sum();
            if (previousExecutedQty < nextRoutingExecutedQty) {
                throw new BusinessException(ErrorCode.PRODUCTION_EXECUTION_ROUTING_MISMATCH);
            }
        }
    }

    private void issueMaterialsForExecution(WorkOrder workOrder, User worker, ProductionExecution execution, int newExecutedQty) {
        int cumulativeExecutedQty = getCurrentExecutedQty(workOrder);
        List<BomRequirement> requirements = bomService.calculateMaterialRequirements(
                workOrder.getItem(),
                workOrder.getBomVersion(),
                cumulativeExecutedQty
        );

        for (BomRequirement requirement : requirements) {
            int requiredDeltaQty = requirement.requiredQty() - getIssuedQty(workOrder, requirement.item());
            if (requiredDeltaQty <= 0) {
                continue;
            }
            int totalAvailableQty = currentInventoryRepository.findByItem(requirement.item()).stream()
                    .mapToInt(CurrentInventory::getCurrentQty)
                    .sum();
            if (totalAvailableQty < requiredDeltaQty) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        for (BomRequirement requirement : requirements) {
            ItemMaster childItem = requirement.item();
            int remainingQty = requirement.requiredQty() - getIssuedQty(workOrder, childItem);
            if (remainingQty <= 0) {
                continue;
            }

            List<CurrentInventory> inventories = currentInventoryRepository.findByItemForUpdate(childItem);
            for (CurrentInventory inventory : inventories) {
                if (remainingQty <= 0) break;
                if (inventory.getCurrentQty() > 0) {
                    int deductQty = Math.min(inventory.getCurrentQty(), remainingQty);
                    inventory.setCurrentQty(inventory.getCurrentQty() - deductQty);
                    currentInventoryRepository.save(inventory);

                    remainingQty -= deductQty;

                    saveHistory(childItem, inventory.getLocation(), TransactionType.PRODUCTION_ISSUE, deductQty, issueReason(workOrder), worker, workOrder, execution);
                }
            }

            if (remainingQty > 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }
    }

    private int getCurrentExecutedQty(WorkOrder workOrder) {
        return productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder).stream()
                .mapToInt(execution -> execution.getGoodQty() + execution.getDefectQty())
                .sum();
    }

    private int getIssuedQty(WorkOrder workOrder, ItemMaster item) {
        return transactionHistoryRepository.findByWorkOrderOrderByTransactionIdAsc(workOrder).stream()
                .filter(history -> history.getItem().getItemId().equals(item.getItemId()))
                .mapToInt(history -> {
                    if (history.getTransactionType() == TransactionType.PRODUCTION_ISSUE) {
                        return history.getQuantity();
                    }
                    if (history.getTransactionType() == TransactionType.PRODUCTION_ISSUE_CANCEL) {
                        return -history.getQuantity();
                    }
                    return 0;
                })
                .sum();
    }

    private String issueReason(WorkOrder workOrder) {
        return "Production issue for WorkOrder: " + workOrder.getOrderNo();
    }

    private void receiveProduction(
            WorkOrder workOrder,
            User worker,
            ProductionExecution execution,
            Integer goodQty,
            String receiptLocationCode
    ) {
        if (goodQty == null || goodQty <= 0) {
            return;
        }
        WarehouseLocation location = findReceiptLocation(receiptLocationCode);
        CurrentInventory inventory = currentInventoryRepository.findByItemAndLocationAndLotNumberIsNullForUpdate(workOrder.getItem(), location)
                .orElseGet(() -> {
                    CurrentInventory ci = new CurrentInventory();
                    ci.setItem(workOrder.getItem());
                    ci.setLocation(location);
                    ci.setCurrentQty(0);
                    return ci;
                });
        inventory.setCurrentQty(inventory.getCurrentQty() + goodQty);
        currentInventoryRepository.save(inventory);
        saveHistory(
                workOrder.getItem(),
                location,
                TransactionType.PRODUCTION_RECEIPT,
                goodQty,
                "Production receipt for WorkOrder: " + workOrder.getOrderNo(),
                worker,
                workOrder,
                execution,
                null
        );
    }

    private WarehouseLocation findReceiptLocation(String receiptLocationCode) {
        String normalized = normalize(receiptLocationCode);
        if (normalized != null) {
            return warehouseLocationRepository.findByLocationCode(normalized)
                    .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));
        }
        return warehouseLocationRepository.findFirstByProductionReceiptDefaultTrueOrderByLocationIdAsc()
                .or(() -> warehouseLocationRepository.findFirstByOrderByLocationIdAsc())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));
    }

    private void restoreInventory(
            ItemMaster item,
            WarehouseLocation location,
            int quantity,
            User worker,
            ProductionExecution execution,
            TransactionType transactionType,
            String reasonDesc,
            InventoryTransactionHistory originalTransaction
    ) {
        CurrentInventory inventory = currentInventoryRepository.findByItemAndLocationForUpdate(item, location)
                .orElseGet(() -> {
                    CurrentInventory ci = new CurrentInventory();
                    ci.setItem(item);
                    ci.setLocation(location);
                    ci.setCurrentQty(0);
                    return ci;
                });
        inventory.setCurrentQty(inventory.getCurrentQty() + quantity);
        currentInventoryRepository.save(inventory);
        saveHistory(item, location, transactionType, quantity, reasonDesc, worker, execution.getOrder(), null, originalTransaction);
    }

    private void deductInventory(
            ItemMaster item,
            WarehouseLocation location,
            int quantity,
            User worker,
            ProductionExecution execution,
            TransactionType transactionType,
            String reasonDesc,
            InventoryTransactionHistory originalTransaction
    ) {
        CurrentInventory inventory = currentInventoryRepository.findByItemAndLocationForUpdate(item, location)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        if (inventory.getCurrentQty() < quantity) {
            throw new BusinessException(ErrorCode.INVENTORY_QTY_NEGATIVE);
        }
        inventory.setCurrentQty(inventory.getCurrentQty() - quantity);
        currentInventoryRepository.save(inventory);
        saveHistory(item, location, transactionType, -quantity, reasonDesc, worker, execution.getOrder(), null, originalTransaction);
    }

    private void saveHistory(
            ItemMaster item,
            WarehouseLocation location,
            TransactionType transactionType,
            int quantity,
            String reasonDesc,
            User worker,
            WorkOrder workOrder,
            ProductionExecution execution
    ) {
        saveHistory(item, location, transactionType, quantity, reasonDesc, worker, workOrder, execution, null);
    }

    private void saveHistory(
            ItemMaster item,
            WarehouseLocation location,
            TransactionType transactionType,
            int quantity,
            String reasonDesc,
            User worker,
            WorkOrder workOrder,
            ProductionExecution execution,
            InventoryTransactionHistory originalTransaction
    ) {
        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(item);
        history.setLocation(location);
        history.setTransactionType(transactionType);
        history.setQuantity(quantity);
        history.setReasonDesc(reasonDesc);
        history.setWorker(worker);
        history.setWorkOrder(workOrder);
        history.setProductionExecution(execution);
        history.setOriginalTransaction(originalTransaction);
        transactionHistoryRepository.save(history);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

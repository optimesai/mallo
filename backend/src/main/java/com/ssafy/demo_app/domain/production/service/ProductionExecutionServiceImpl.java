package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionCreateRequest;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.bom.repository.BomStructureRepository;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionExecutionServiceImpl implements ProductionExecutionService {

    private final ProductionExecutionRepository productionExecutionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final FactoryRoutingRepository factoryRoutingRepository;
    private final BomStructureRepository bomStructureRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;

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

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        FactoryRouting routing = factoryRoutingRepository.findById(request.getRoutingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTING_NOT_FOUND));

        issueMaterialsForExecution(workOrder, worker, request.getGoodQty() + request.getDefectQty());

        ProductionExecution execution = new ProductionExecution();
        execution.setOrder(workOrder);
        execution.setRouting(routing);
        execution.setGoodQty(request.getGoodQty());
        execution.setDefectQty(request.getDefectQty());
        execution.setWorker(worker);
        execution.setManHoursMinutes(request.getManHoursMinutes());

        return ProductionExecutionResponse.from(productionExecutionRepository.save(execution));
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
        productionExecutionRepository.delete(execution);
    }

    private WorkOrder findWorkOrder(String orderKey) {
        if (orderKey != null && orderKey.matches("\\d+")) {
            try {
                return workOrderRepository.findById(Integer.valueOf(orderKey))
                        .orElseThrow(() -> new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND));
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
            }
        }
        return workOrderRepository.findByOrderNo(orderKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND));
    }

    private ProductionExecution findExecution(Integer executionId) {
        return productionExecutionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCTION_EXECUTION_NOT_FOUND));
    }

    private void issueMaterialsForExecution(WorkOrder workOrder, User worker, int newExecutedQty) {
        List<BomStructure> bomList = bomStructureRepository.findByParentItem(workOrder.getItem());
        if (bomList.isEmpty()) {
            throw new BusinessException(ErrorCode.BOM_NOT_FOUND);
        }

        int cumulativeExecutedQty = getCurrentExecutedQty(workOrder) + newExecutedQty;
        for (BomStructure bom : bomList) {
            int requiredDeltaQty = calculateRequiredQty(bom, cumulativeExecutedQty) - getIssuedQty(workOrder, bom.getChildItem());
            if (requiredDeltaQty <= 0) {
                continue;
            }
            int totalAvailableQty = currentInventoryRepository.findByItem(bom.getChildItem()).stream()
                    .mapToInt(CurrentInventory::getCurrentQty)
                    .sum();
            if (totalAvailableQty < requiredDeltaQty) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        for (BomStructure bom : bomList) {
            ItemMaster childItem = bom.getChildItem();
            int remainingQty = calculateRequiredQty(bom, cumulativeExecutedQty) - getIssuedQty(workOrder, childItem);
            if (remainingQty <= 0) {
                continue;
            }

            List<CurrentInventory> inventories = currentInventoryRepository.findByItem(childItem);
            for (CurrentInventory inventory : inventories) {
                if (remainingQty <= 0) break;
                if (inventory.getCurrentQty() > 0) {
                    int deductQty = Math.min(inventory.getCurrentQty(), remainingQty);
                    inventory.setCurrentQty(inventory.getCurrentQty() - deductQty);
                    currentInventoryRepository.save(inventory);

                    remainingQty -= deductQty;

                    InventoryTransactionHistory history = new InventoryTransactionHistory();
                    history.setItem(childItem);
                    history.setLocation(inventory.getLocation());
                    history.setTransactionType(InventoryTransactionHistory.TransactionType.PRODUCTION_ISSUE);
                    history.setQuantity(deductQty);
                    history.setReasonDesc(issueReason(workOrder));
                    history.setWorker(worker);
                    transactionHistoryRepository.save(history);
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

    private int calculateRequiredQty(BomStructure bom, Integer targetQty) {
        return bom.getQuantity().multiply(BigDecimal.valueOf(targetQty)).intValue();
    }

    private int getIssuedQty(WorkOrder workOrder, ItemMaster item) {
        return transactionHistoryRepository.findByReasonDescContainingOrderByTransactionIdAsc(workOrder.getOrderNo()).stream()
                .filter(history -> history.getTransactionType() == InventoryTransactionHistory.TransactionType.PRODUCTION_ISSUE)
                .filter(history -> history.getItem().getItemId().equals(item.getItemId()))
                .mapToInt(InventoryTransactionHistory::getQuantity)
                .sum();
    }

    private String issueReason(WorkOrder workOrder) {
        return "Production issue for WorkOrder: " + workOrder.getOrderNo();
    }
}

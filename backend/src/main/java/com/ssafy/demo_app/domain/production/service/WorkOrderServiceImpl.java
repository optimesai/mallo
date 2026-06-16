package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderCloseRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderCreateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderDetailResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderExecutionSummary;
import com.ssafy.demo_app.api.production.dto.WorkOrderMaterialRequirementResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderStatusUpdateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderUpdateRequest;
import com.ssafy.demo_app.domain.bom.service.BomRequirement;
import com.ssafy.demo_app.domain.bom.service.BomService;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String DEFAULT_BOM_VERSION = "v1.0";

    private final WorkOrderRepository workOrderRepository;
    private final BomService bomService;
    private final UserRepository userRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;
    private final ItemMasterRepository itemMasterRepository;
    private final FactoryRoutingRepository factoryRoutingRepository;
    private final ProductionExecutionRepository productionExecutionRepository;

    @Override
    @Transactional
    public WorkOrderResponse createWorkOrder(WorkOrderCreateRequest request) {
        ItemMaster item = findProductionItem(request.getItemCode());
        FactoryRouting routing = findRouting(request.getRoutingId());
        String bomVersion = normalizeBomVersion(request.getBomVersion());
        bomService.validateActiveBomVersion(item, bomVersion);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(generateOrderNo(request.getPlanDate()));
        workOrder.setItem(item);
        workOrder.setRouting(routing);
        workOrder.setTargetQty(request.getTargetQty());
        workOrder.setBomVersion(bomVersion);
        workOrder.setPlanDate(request.getPlanDate());
        workOrder.setStatus(WorkOrder.OrderStatus.READY);

        return toResponse(workOrderRepository.save(workOrder));
    }

    @Override
    public List<WorkOrderResponse> getWorkOrders(
            WorkOrder.OrderStatus status,
            LocalDate planDate,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            String factoryName,
            String lineName
    ) {
        String normalizedKeyword = normalize(keyword);
        String normalizedFactoryName = normalize(factoryName);
        String normalizedLineName = normalize(lineName);

        return workOrderRepository.searchWorkOrders(
                        status,
                        planDate,
                        fromDate,
                        toDate,
                        normalizedKeyword,
                        normalizedFactoryName,
                        normalizedLineName
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public WorkOrderDetailResponse getWorkOrder(String orderKey) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        List<ProductionExecutionResponse> executions = productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder).stream()
                .map(ProductionExecutionResponse::from)
                .toList();
        return new WorkOrderDetailResponse(
                toResponse(workOrder),
                getMaterialRequirements(workOrder),
                executions
        );
    }

    @Override
    @Transactional
    public WorkOrderResponse updateWorkOrder(String orderKey, WorkOrderUpdateRequest request) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        validateEditable(workOrder);

        ItemMaster item = findProductionItem(request.getItemCode());
        String bomVersion = normalizeBomVersion(request.getBomVersion());
        bomService.validateActiveBomVersion(item, bomVersion);

        workOrder.setItem(item);
        workOrder.setRouting(findRouting(request.getRoutingId()));
        workOrder.setTargetQty(request.getTargetQty());
        workOrder.setBomVersion(bomVersion);
        workOrder.setPlanDate(request.getPlanDate());

        return toResponse(workOrderRepository.save(workOrder));
    }

    @Override
    @Transactional
    public void deleteWorkOrder(String orderKey) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        validateEditable(workOrder);
        workOrderRepository.delete(workOrder);
    }

    @Override
    @Transactional
    public WorkOrderResponse updateStatus(String orderKey, WorkOrderStatusUpdateRequest request) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        WorkOrder.OrderStatus nextStatus = request.getStatus();

        if (workOrder.getStatus() == WorkOrder.OrderStatus.CLOSE || nextStatus == WorkOrder.OrderStatus.CLOSE) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        if (workOrder.getStatus() == WorkOrder.OrderStatus.READY && nextStatus == WorkOrder.OrderStatus.RUN) {
            workOrder.setStatus(nextStatus);
            return toResponse(workOrderRepository.save(workOrder));
        }
        if (workOrder.getStatus() == WorkOrder.OrderStatus.RUN && nextStatus == WorkOrder.OrderStatus.HOLD) {
            workOrder.setStatus(nextStatus);
            return toResponse(workOrderRepository.save(workOrder));
        }
        if (workOrder.getStatus() == WorkOrder.OrderStatus.HOLD && nextStatus == WorkOrder.OrderStatus.RUN) {
            workOrder.setStatus(nextStatus);
            return toResponse(workOrderRepository.save(workOrder));
        }
        throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
    }

    @Override
    @Transactional
    public WorkOrderResponse closeWorkOrder(String orderKey, WorkOrderCloseRequest request) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        if (workOrder.getStatus() != WorkOrder.OrderStatus.RUN && workOrder.getStatus() != WorkOrder.OrderStatus.HOLD) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        if (!productionExecutionRepository.existsByOrder(workOrder)) {
            throw new BusinessException(ErrorCode.PRODUCTION_EXECUTION_NOT_FOUND);
        }

        WorkOrderExecutionSummary summary = summarize(workOrder);
        boolean allowUnderTargetClose = request != null && Boolean.TRUE.equals(request.getAllowUnderTargetClose());
        if (summary.totalGoodQty() < workOrder.getTargetQty() && !allowUnderTargetClose) {
            throw new BusinessException(ErrorCode.WORK_ORDER_CLOSE_UNDER_TARGET);
        }

        workOrder.setStatus(WorkOrder.OrderStatus.CLOSE);
        return toResponse(workOrderRepository.save(workOrder));
    }

    @Override
    @Transactional
    public void issueMaterials(String orderKey, Integer workerId) {
        WorkOrder workOrder = findWorkOrder(orderKey);
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (workOrder.getStatus() != WorkOrder.OrderStatus.READY && workOrder.getStatus() != WorkOrder.OrderStatus.RUN) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }

        List<BomRequirement> requirements = bomService.calculateMaterialRequirements(
                workOrder.getItem(),
                workOrder.getBomVersion(),
                workOrder.getTargetQty()
        );

        for (BomRequirement requirement : requirements) {
            int requiredQty = requirement.requiredQty() - getIssuedQty(workOrder, requirement.item());
            if (requiredQty <= 0) {
                continue;
            }
            int totalAvailableQty = currentInventoryRepository.findByItem(requirement.item()).stream()
                    .mapToInt(CurrentInventory::getCurrentQty)
                    .sum();

            if (totalAvailableQty < requiredQty) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        for (BomRequirement requirement : requirements) {
            ItemMaster childItem = requirement.item();
            int remainingQty = requirement.requiredQty() - getIssuedQty(workOrder, childItem);
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
                    history.setTransactionType(TransactionType.PRODUCTION_ISSUE);
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

        if (workOrder.getStatus() == WorkOrder.OrderStatus.READY) {
            workOrder.setStatus(WorkOrder.OrderStatus.RUN);
            workOrderRepository.save(workOrder);
        }
    }

    private String generateOrderNo(LocalDate planDate) {
        String prefix = "WO-" + planDate.format(ORDER_NO_DATE_FORMAT) + "-";
        int nextSequence = workOrderRepository.findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
                .map(WorkOrder::getOrderNo)
                .map(orderNo -> orderNo.substring(orderNo.lastIndexOf('-') + 1))
                .map(Integer::parseInt)
                .orElse(0) + 1;
        return prefix + String.format("%03d", nextSequence);
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

    private ItemMaster findProductionItem(String itemCode) {
        ItemMaster item = itemMasterRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (item.getItemType() == ItemMaster.ItemType.RAW) {
            throw new BusinessException(ErrorCode.WORK_ORDER_INVALID_ITEM_TYPE);
        }
        return item;
    }

    private FactoryRouting findRouting(Integer routingId) {
        FactoryRouting routing = factoryRoutingRepository.findById(routingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTING_NOT_FOUND));
        if (routing.getRoutingStatus() != FactoryRouting.RoutingStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ROUTING_INACTIVE);
        }
        return routing;
    }

    private void validateEditable(WorkOrder workOrder) {
        if (workOrder.getStatus() != WorkOrder.OrderStatus.READY) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        if (productionExecutionRepository.existsByOrder(workOrder)) {
            throw new BusinessException(ErrorCode.WORK_ORDER_HAS_EXECUTIONS);
        }
        if (transactionHistoryRepository.existsByReasonDescContaining(workOrder.getOrderNo())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_HAS_ISSUES);
        }
    }

    private WorkOrderResponse toResponse(WorkOrder workOrder) {
        return WorkOrderResponse.from(workOrder, summarize(workOrder));
    }

    private WorkOrderExecutionSummary summarize(WorkOrder workOrder) {
        List<ProductionExecution> executions = productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder);
        int goodQty = executions.stream().mapToInt(ProductionExecution::getGoodQty).sum();
        int defectQty = executions.stream().mapToInt(ProductionExecution::getDefectQty).sum();
        int manHours = executions.stream().mapToInt(ProductionExecution::getManHoursMinutes).sum();
        return new WorkOrderExecutionSummary(goodQty, defectQty, manHours);
    }

    private List<WorkOrderMaterialRequirementResponse> getMaterialRequirements(WorkOrder workOrder) {
        return bomService.calculateMaterialRequirements(
                        workOrder.getItem(),
                        workOrder.getBomVersion(),
                        workOrder.getTargetQty()
                )
                .stream()
                .map(requirement -> {
                    ItemMaster childItem = requirement.item();
                    int issuedQty = transactionHistoryRepository.findByReasonDescContainingOrderByTransactionIdAsc(workOrder.getOrderNo()).stream()
                            .filter(history -> history.getItem().getItemId().equals(childItem.getItemId()))
                            .mapToInt(InventoryTransactionHistory::getQuantity)
                            .sum();
                    int availableQty = currentInventoryRepository.findByItem(childItem).stream()
                            .mapToInt(CurrentInventory::getCurrentQty)
                            .sum();
                    return new WorkOrderMaterialRequirementResponse(
                            childItem.getItemId(),
                            childItem.getItemCode(),
                            childItem.getItemName(),
                            childItem.getItemType().name(),
                            childItem.getUnit().name(),
                            requirement.bomQuantity(),
                            requirement.requiredQty(),
                            issuedQty,
                            availableQty
                    );
                })
                .toList();
    }

    private int getIssuedQty(WorkOrder workOrder, ItemMaster item) {
        return transactionHistoryRepository.findByReasonDescContainingOrderByTransactionIdAsc(workOrder.getOrderNo()).stream()
                .filter(history -> history.getTransactionType() == TransactionType.PRODUCTION_ISSUE)
                .filter(history -> history.getItem().getItemId().equals(item.getItemId()))
                .mapToInt(InventoryTransactionHistory::getQuantity)
                .sum();
    }

    private boolean matchesKeyword(WorkOrder order, String keyword) {
        return order.getOrderNo().toLowerCase(Locale.ROOT).contains(keyword)
                || order.getItem().getItemCode().toLowerCase(Locale.ROOT).contains(keyword)
                || order.getItem().getItemName().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeBomVersion(String bomVersion) {
        return bomVersion == null || bomVersion.isBlank() ? DEFAULT_BOM_VERSION : bomVersion.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String issueReason(WorkOrder workOrder) {
        return "Production issue for WorkOrder: " + workOrder.getOrderNo();
    }
}

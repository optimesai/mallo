package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.api.production.dto.ProductionIssueHistoryResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderCloseRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderCreateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderDetailResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderExecutionSummary;
import com.ssafy.demo_app.api.production.dto.WorkOrderMaterialRequirementResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderOperationProgressResponse;
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
import com.ssafy.demo_app.domain.production.entity.WorkOrderSequence;
import com.ssafy.demo_app.domain.production.repository.ProductionExecutionRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderSequenceRepository;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.domain.routing.repository.FactoryRoutingRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final WorkOrderSequenceRepository workOrderSequenceRepository;

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
    public PageResponse<WorkOrderResponse> getWorkOrders(
            Pageable pageable,
            WorkOrder.OrderStatus status,
            LocalDate planDate,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            String itemKeyword,
            String factoryName,
            String lineName,
            String operationName
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String normalizedKeyword = normalize(keyword);
        String normalizedItemKeyword = normalize(itemKeyword);
        String normalizedFactoryName = normalize(factoryName);
        String normalizedLineName = normalize(lineName);
        String normalizedOperationName = normalize(operationName);

        List<WorkOrderResponse> responses = new ArrayList<>(workOrderRepository.searchWorkOrders(
                        status,
                        planDate,
                        fromDate,
                        toDate,
                        normalizedKeyword,
                        normalizedItemKeyword,
                        normalizedFactoryName,
                        normalizedLineName,
                        normalizedOperationName,
                        Pageable.unpaged()
                )
                .map(this::toListResponse)
                .getContent());
        responses.sort(workOrderComparator(pageable));
        return PageResponse.from(toPage(responses, pageable));
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
                getOperationProgresses(workOrder),
                executions,
                getIssueHistories(workOrder)
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
        WorkOrder workOrder = findWorkOrderForUpdate(orderKey);
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (workOrder.getStatus() != WorkOrder.OrderStatus.READY) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }

        issueMaterialsForQuantity(workOrder, worker, null, workOrder.getTargetQty());

        workOrder.setStatus(WorkOrder.OrderStatus.RUN);
        workOrderRepository.save(workOrder);
    }

    @Override
    @Transactional
    public void cancelIssueMaterials(String orderKey, Integer workerId) {
        WorkOrder workOrder = findWorkOrderForUpdate(orderKey);
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (workOrder.getStatus() == WorkOrder.OrderStatus.CLOSE) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        if (productionExecutionRepository.existsByOrder(workOrder)) {
            throw new BusinessException(ErrorCode.WORK_ORDER_HAS_EXECUTIONS);
        }

        List<InventoryTransactionHistory> issueHistories = transactionHistoryRepository.findByWorkOrderOrderByTransactionIdAsc(workOrder).stream()
                .filter(history -> history.getTransactionType() == TransactionType.PRODUCTION_ISSUE)
                .filter(history -> history.getProductionExecution() == null)
                .toList();
        boolean alreadyCanceled = transactionHistoryRepository.findByWorkOrderOrderByTransactionIdAsc(workOrder).stream()
                .anyMatch(history -> history.getTransactionType() == TransactionType.PRODUCTION_ISSUE_CANCEL
                        && history.getProductionExecution() == null);

        if (issueHistories.isEmpty() || alreadyCanceled) {
            throw new BusinessException(ErrorCode.WORK_ORDER_HAS_ISSUES);
        }

        for (InventoryTransactionHistory issueHistory : issueHistories) {
            restoreInventory(
                    issueHistory.getItem(),
                    issueHistory.getLocation(),
                    issueHistory.getQuantity(),
                    worker,
                    workOrder,
                    null,
                    TransactionType.PRODUCTION_ISSUE_CANCEL,
                    "Production issue cancel for WorkOrder: " + workOrder.getOrderNo(),
                    issueHistory
            );
        }

        if (workOrder.getStatus() == WorkOrder.OrderStatus.RUN) {
            workOrder.setStatus(WorkOrder.OrderStatus.READY);
            workOrderRepository.save(workOrder);
        }
    }

    private synchronized String generateOrderNo(LocalDate planDate) {
        WorkOrderSequence sequence = findOrCreateSequence(planDate);
        sequence.setLastSequence(sequence.getLastSequence() + 1);
        workOrderSequenceRepository.save(sequence);

        String prefix = "WO-" + planDate.format(ORDER_NO_DATE_FORMAT) + "-";
        return prefix + String.format("%03d", sequence.getLastSequence());
    }

    private WorkOrderSequence findOrCreateSequence(LocalDate planDate) {
        return workOrderSequenceRepository.findByPlanDateForUpdate(planDate)
                .orElseGet(() -> {
                    try {
                        WorkOrderSequence sequence = new WorkOrderSequence();
                        sequence.setPlanDate(planDate);
                        sequence.setLastSequence(0);
                        return workOrderSequenceRepository.saveAndFlush(sequence);
                    } catch (DataIntegrityViolationException exception) {
                        return workOrderSequenceRepository.findByPlanDateForUpdate(planDate)
                                .orElseThrow(() -> exception);
                    }
                });
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

    private WorkOrder findWorkOrderForUpdate(String orderKey) {
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
        if (transactionHistoryRepository.existsByWorkOrder(workOrder)) {
            throw new BusinessException(ErrorCode.WORK_ORDER_HAS_ISSUES);
        }
    }

    private WorkOrderResponse toResponse(WorkOrder workOrder) {
        return WorkOrderResponse.from(
                workOrder,
                summarize(workOrder),
                canCancelIssueMaterials(workOrder),
                canDeleteExecution(workOrder),
                getCurrentOperationProgress(workOrder)
        );
    }

    private WorkOrderResponse toListResponse(WorkOrder workOrder) {
        return WorkOrderResponse.from(
                workOrder,
                summarize(workOrder),
                false,
                canDeleteExecution(workOrder)
        );
    }

    private Page<WorkOrderResponse> toPage(List<WorkOrderResponse> responses, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(responses);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<WorkOrderResponse> content = start >= responses.size() ? List.of() : responses.subList(start, end);
        return new PageImpl<>(
                content,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
                responses.size()
        );
    }

    private Comparator<WorkOrderResponse> workOrderComparator(Pageable pageable) {
        Sort.Order order = firstOrder(pageable, "planDate");
        Comparator<WorkOrderResponse> comparator = switch (order.getProperty()) {
            case "orderId" -> Comparator.comparing(WorkOrderResponse::getOrderId, Comparator.nullsLast(Integer::compareTo));
            case "orderNo" -> Comparator.comparing(WorkOrderResponse::getOrderNo, this::compareText);
            case "itemCode" -> Comparator.comparing(WorkOrderResponse::getItemCode, this::compareText);
            case "itemName" -> Comparator.comparing(WorkOrderResponse::getItemName, this::compareText);
            case "factoryName" -> Comparator.comparing(WorkOrderResponse::getFactoryName, this::compareText);
            case "lineName" -> Comparator.comparing(WorkOrderResponse::getLineName, this::compareText);
            case "operationSeq" -> Comparator.comparing(WorkOrderResponse::getOperationSeq, Comparator.nullsLast(Integer::compareTo));
            case "operationName" -> Comparator.comparing(WorkOrderResponse::getOperationName, this::compareText);
            case "bomVersion" -> this::compareBomVersion;
            case "targetQty" -> Comparator.comparing(WorkOrderResponse::getTargetQty, Comparator.nullsLast(Integer::compareTo));
            case "totalExecutedQty" -> Comparator.comparing(WorkOrderResponse::getTotalExecutedQty, Comparator.nullsLast(Integer::compareTo));
            case "progressRate" -> Comparator.comparing(WorkOrderResponse::getProgressRate, Comparator.nullsLast(Double::compareTo));
            case "status" -> Comparator.comparingInt(response -> statusLabelOrder(response.getStatus()));
            case "createdAt" -> Comparator.comparing(WorkOrderResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(WorkOrderResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(WorkOrderResponse::getPlanDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (order.getDirection().isDescending()) {
            comparator = comparator.reversed();
        }
        return comparator
                .thenComparing(WorkOrderResponse::getPlanDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(WorkOrderResponse::getOrderNo, this::compareText);
    }

    private Sort.Order firstOrder(Pageable pageable, String defaultProperty) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return Sort.Order.desc(defaultProperty);
        }
        return pageable.getSort().iterator().next();
    }

    private int compareBomVersion(WorkOrderResponse left, WorkOrderResponse right) {
        return compareVersion(left.getBomVersion(), right.getBomVersion());
    }

    private int compareVersion(String left, String right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        String[] leftParts = left.replaceFirst("^[^0-9]+", "").split("\\.");
        String[] rightParts = right.replaceFirst("^[^0-9]+", "").split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int leftNumber = parseVersionPart(leftParts, i);
            int rightNumber = parseVersionPart(rightParts, i);
            if (leftNumber != rightNumber) {
                return Integer.compare(leftNumber, rightNumber);
            }
        }
        return left.compareToIgnoreCase(right);
    }

    private int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private int statusLabelOrder(String status) {
        if ("CLOSE".equals(status)) return 1;
        if ("READY".equals(status)) return 2;
        if ("HOLD".equals(status)) return 3;
        if ("RUN".equals(status)) return 4;
        return 99;
    }

    private int compareText(String left, String right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        return left.compareToIgnoreCase(right);
    }

    private boolean canCancelIssueMaterials(WorkOrder workOrder) {
        if (workOrder.getStatus() == WorkOrder.OrderStatus.CLOSE || productionExecutionRepository.existsByOrder(workOrder)) {
            return false;
        }
        List<InventoryTransactionHistory> histories = transactionHistoryRepository.findByWorkOrderOrderByTransactionIdAsc(workOrder);
        boolean hasWorkOrderIssue = histories.stream()
                .anyMatch(history -> history.getTransactionType() == TransactionType.PRODUCTION_ISSUE
                        && history.getProductionExecution() == null);
        boolean hasWorkOrderIssueCancel = histories.stream()
                .anyMatch(history -> history.getTransactionType() == TransactionType.PRODUCTION_ISSUE_CANCEL
                        && history.getProductionExecution() == null);
        return hasWorkOrderIssue && !hasWorkOrderIssueCancel;
    }

    private boolean canDeleteExecution(WorkOrder workOrder) {
        return workOrder.getStatus() != WorkOrder.OrderStatus.CLOSE
                && productionExecutionRepository.existsByOrder(workOrder);
    }

    private WorkOrderExecutionSummary summarize(WorkOrder workOrder) {
        List<ProductionExecution> executions = productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder);
        FactoryRouting lastRouting = getLineRoutings(workOrder).stream()
                .reduce((first, second) -> second)
                .orElse(workOrder.getRouting());
        int goodQty = executions.stream()
                .filter(execution -> hasRouting(execution, lastRouting))
                .mapToInt(ProductionExecution::getGoodQty)
                .sum();
        int defectQty = executions.stream()
                .filter(execution -> hasRouting(execution, lastRouting))
                .mapToInt(ProductionExecution::getDefectQty)
                .sum();
        int manHours = executions.stream().mapToInt(ProductionExecution::getManHoursMinutes).sum();
        return new WorkOrderExecutionSummary(goodQty, defectQty, manHours);
    }

    private List<WorkOrderOperationProgressResponse> getOperationProgresses(WorkOrder workOrder) {
        List<ProductionExecution> executions = productionExecutionRepository.findByOrderOrderByExecutionIdAsc(workOrder);
        List<FactoryRouting> lineRoutings = getLineRoutings(workOrder);
        if (lineRoutings.isEmpty()) {
            return List.of();
        }

        boolean currentOperationAssigned = false;
        int previousGoodQty = 0;
        List<WorkOrderOperationProgressResponse> progresses = new java.util.ArrayList<>();
        for (int index = 0; index < lineRoutings.size(); index++) {
            FactoryRouting routing = lineRoutings.get(index);
            int availableQty = index == 0
                    ? getIssuedProductionCapacity(workOrder)
                    : previousGoodQty;
            int goodQty = executions.stream()
                    .filter(execution -> hasRouting(execution, routing))
                    .mapToInt(ProductionExecution::getGoodQty)
                    .sum();
            int defectQty = executions.stream()
                    .filter(execution -> hasRouting(execution, routing))
                    .mapToInt(ProductionExecution::getDefectQty)
                    .sum();
            boolean currentOperation = !currentOperationAssigned
                    && workOrder.getStatus() == WorkOrder.OrderStatus.RUN
                    && availableQty > goodQty + defectQty;
            if (currentOperation) {
                currentOperationAssigned = true;
            }

            progresses.add(new WorkOrderOperationProgressResponse(
                    routing.getRoutingId(),
                    routing.getFactoryName(),
                    routing.getLineName(),
                    routing.getOperationSeq(),
                    routing.getOperationName(),
                    workOrder.getTargetQty(),
                    availableQty,
                    goodQty,
                    defectQty,
                    currentOperation
            ));
            previousGoodQty = goodQty;
        }
        return progresses;
    }

    private WorkOrderOperationProgressResponse getCurrentOperationProgress(WorkOrder workOrder) {
        List<WorkOrderOperationProgressResponse> progresses = getOperationProgresses(workOrder);
        if (progresses.isEmpty()) {
            return null;
        }
        return progresses.stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getCurrentOperation()))
                .findFirst()
                .or(() -> progresses.stream()
                        .filter(progress -> progress.getCompletedQty() < progress.getTargetQty())
                        .findFirst())
                .orElse(progresses.get(progresses.size() - 1));
    }

    private List<FactoryRouting> getLineRoutings(WorkOrder workOrder) {
        FactoryRouting routing = workOrder.getRouting();
        return factoryRoutingRepository.findByFactoryNameAndLineNameOrderByOperationSeqAsc(
                routing.getFactoryName(),
                routing.getLineName()
        );
    }

    private boolean hasRouting(ProductionExecution execution, FactoryRouting routing) {
        return execution.getRouting() != null
                && execution.getRouting().getRoutingId().equals(routing.getRoutingId());
    }

    private int getIssuedProductionCapacity(WorkOrder workOrder) {
        List<BomRequirement> requirements;
        try {
            requirements = bomService.calculateMaterialRequirements(
                    workOrder.getItem(),
                    workOrder.getBomVersion(),
                    workOrder.getTargetQty()
            );
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.BOM_NOT_FOUND) {
                return 0;
            }
            throw exception;
        }
        if (requirements.isEmpty()) {
            return workOrder.getTargetQty();
        }
        return requirements.stream()
                .mapToInt(requirement -> getIssuedQty(workOrder, requirement.item()) / requirement.bomQuantity())
                .min()
                .orElse(0);
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
                    int issuedQty = getIssuedQty(workOrder, childItem);
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

    private List<ProductionIssueHistoryResponse> getIssueHistories(WorkOrder workOrder) {
        return transactionHistoryRepository.findByWorkOrderOrderByTransactionIdAsc(workOrder).stream()
                .map(ProductionIssueHistoryResponse::from)
                .toList();
    }

    private void issueMaterialsForQuantity(WorkOrder workOrder, User worker, ProductionExecution execution, int productionQty) {
        List<BomRequirement> requirements = bomService.calculateMaterialRequirements(
                workOrder.getItem(),
                workOrder.getBomVersion(),
                productionQty
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
            for (CurrentInventory inventory : currentInventoryRepository.findByItemForUpdate(childItem)) {
                if (remainingQty <= 0) break;
                if (inventory.getCurrentQty() > 0) {
                    int deductQty = Math.min(inventory.getCurrentQty(), remainingQty);
                    inventory.setCurrentQty(inventory.getCurrentQty() - deductQty);
                    currentInventoryRepository.save(inventory);
                    remainingQty -= deductQty;
                    saveHistory(childItem, inventory.getLocation(), TransactionType.PRODUCTION_ISSUE, deductQty, issueReason(workOrder), worker, workOrder, execution, null);
                }
            }
            if (remainingQty > 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }
    }

    private void restoreInventory(
            ItemMaster item,
            com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation location,
            int quantity,
            User worker,
            WorkOrder workOrder,
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
        saveHistory(item, location, transactionType, quantity, reasonDesc, worker, workOrder, execution, originalTransaction);
    }

    private void saveHistory(
            ItemMaster item,
            com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation location,
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
            com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation location,
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

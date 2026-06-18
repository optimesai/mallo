package com.ssafy.demo_app.domain.routing.service;

import com.ssafy.demo_app.api.routing.dto.FactoryRoutingRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingStatusUpdateRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingTreeResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingUsageResponse;
import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.production.repository.ProductionExecutionRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.domain.routing.repository.FactoryRoutingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FactoryRoutingServiceImpl implements FactoryRoutingService {

    private final FactoryRoutingRepository factoryRoutingRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ProductionExecutionRepository productionExecutionRepository;

    @Override
    public List<FactoryRoutingResponse> getRoutings(
            String factoryName,
            String lineName,
            FactoryRouting.RoutingStatus routingStatus
    ) {
        List<FactoryRouting> routings;
        if (hasText(factoryName) && hasText(lineName)) {
            routings = factoryRoutingRepository.findByFactoryNameAndLineNameOrderByOperationSeqAsc(
                    factoryName.trim(),
                    lineName.trim()
            );
        } else if (hasText(factoryName)) {
            routings = factoryRoutingRepository.findByFactoryNameOrderByLineNameAscOperationSeqAsc(factoryName.trim());
        } else if (routingStatus != null) {
            routings = factoryRoutingRepository.findByRoutingStatusOrderByFactoryNameAscLineNameAscOperationSeqAsc(routingStatus);
        } else {
            routings = factoryRoutingRepository.findAllByOrderByFactoryNameAscLineNameAscOperationSeqAsc();
        }

        return routings.stream()
                .filter(routing -> routingStatus == null || routing.getRoutingStatus() == routingStatus)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FactoryRoutingResponse getRouting(Integer routingId) {
        return toResponse(findRouting(routingId));
    }

    @Override
    @Transactional
    public FactoryRoutingResponse createRouting(FactoryRoutingRequest request) {
        String factoryName = trimRequired(request.getFactoryName());
        String lineName = trimRequired(request.getLineName());
        String operationName = trimRequired(request.getOperationName());

        validateDuplicate(factoryName, lineName, request.getOperationSeq());

        FactoryRouting routing = new FactoryRouting();
        routing.setFactoryName(factoryName);
        routing.setLineName(lineName);
        routing.setOperationSeq(request.getOperationSeq());
        routing.setOperationName(operationName);

        return toResponse(saveRouting(routing));
    }

    @Override
    @Transactional
    public FactoryRoutingResponse updateRouting(Integer routingId, FactoryRoutingRequest request) {
        FactoryRouting routing = findRouting(routingId);
        validateNoReference(routing);
        String factoryName = trimRequired(request.getFactoryName());
        String lineName = trimRequired(request.getLineName());
        String operationName = trimRequired(request.getOperationName());

        validateDuplicateExceptSelf(factoryName, lineName, request.getOperationSeq(), routingId);

        routing.setFactoryName(factoryName);
        routing.setLineName(lineName);
        routing.setOperationSeq(request.getOperationSeq());
        routing.setOperationName(operationName);

        return toResponse(saveRouting(routing));
    }

    @Override
    @Transactional
    public void deleteRouting(Integer routingId) {
        FactoryRouting routing = findRouting(routingId);
        validateNoReference(routing);
        factoryRoutingRepository.delete(routing);
    }

    @Override
    @Transactional
    public FactoryRoutingResponse updateRoutingStatus(Integer routingId, FactoryRoutingStatusUpdateRequest request) {
        FactoryRouting routing = findRouting(routingId);
        routing.setRoutingStatus(request.getRoutingStatus());
        return toResponse(saveRouting(routing));
    }

    @Override
    public FactoryRoutingUsageResponse getRoutingUsage(Integer routingId) {
        FactoryRouting routing = findRouting(routingId);
        long workOrderCount = workOrderRepository.countByRouting(routing);
        long executionCount = productionExecutionRepository.countByRouting(routing);
        List<String> workOrderNos = workOrderRepository.findTop5ByRoutingOrderByOrderIdDesc(routing)
                .stream()
                .map(WorkOrder::getOrderNo)
                .toList();
        List<Integer> executionIds = productionExecutionRepository.findTop5ByRoutingOrderByExecutionIdDesc(routing)
                .stream()
                .map(ProductionExecution::getExecutionId)
                .toList();
        boolean hasReference = workOrderCount > 0 || executionCount > 0;
        String recommendedAction = hasReference
                ? "참조 이력이 있으므로 삭제 또는 수정 대신 비활성화를 사용하세요."
                : "참조 이력이 없어 수정 또는 삭제할 수 있습니다.";
        return new FactoryRoutingUsageResponse(
                routing.getRoutingId(),
                workOrderCount,
                executionCount,
                workOrderNos,
                executionIds,
                !hasReference,
                !hasReference,
                recommendedAction
        );
    }

    @Override
    public List<String> getFactories() {
        return factoryRoutingRepository.findDistinctFactoryNames();
    }

    @Override
    public List<String> getLines(String factoryName) {
        return factoryRoutingRepository.findDistinctLineNamesByFactoryName(trimRequired(factoryName));
    }

    @Override
    public List<FactoryRoutingResponse> getOperations(String factoryName, String lineName) {
        return factoryRoutingRepository
                .findByFactoryNameAndLineNameOrderByOperationSeqAsc(
                        trimRequired(factoryName),
                        trimRequired(lineName)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<FactoryRoutingTreeResponse> getRoutingTree() {
        List<FactoryRouting> routings = factoryRoutingRepository.findAllByOrderByFactoryNameAscLineNameAscOperationSeqAsc();
        Map<String, Map<String, List<FactoryRouting>>> tree = new LinkedHashMap<>();

        for (FactoryRouting routing : routings) {
            tree.computeIfAbsent(routing.getFactoryName(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(routing.getLineName(), key -> new ArrayList<>())
                    .add(routing);
        }

        return tree.entrySet().stream()
                .map(factoryEntry -> new FactoryRoutingTreeResponse(
                        factoryEntry.getKey(),
                        toLineResponses(factoryEntry.getValue())
                ))
                .toList();
    }

    private List<FactoryRoutingTreeResponse.LineResponse> toLineResponses(
            Map<String, List<FactoryRouting>> lines
    ) {
        return lines.entrySet().stream()
                .map(lineEntry -> new FactoryRoutingTreeResponse.LineResponse(
                        lineEntry.getKey(),
                        lineEntry.getValue().stream()
                                .map(routing -> new FactoryRoutingTreeResponse.OperationResponse(
                                        routing.getRoutingId(),
                                        routing.getOperationSeq(),
                                        routing.getOperationName()
                                ))
                                .toList()
                ))
                .toList();
    }

    private FactoryRouting findRouting(Integer routingId) {
        return factoryRoutingRepository.findById(routingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTING_NOT_FOUND));
    }

    private FactoryRoutingResponse toResponse(FactoryRouting routing) {
        FactoryRoutingResponse response = FactoryRoutingResponse.from(routing);
        response.setLastExecutionAt(resolveLastExecutionAt(routing));
        return response;
    }

    private LocalDateTime resolveLastExecutionAt(FactoryRouting routing) {
        return productionExecutionRepository.findTopByRoutingOrderByCreatedAtDesc(routing)
                .map(ProductionExecution::getCreatedAt)
                .orElse(null);
    }

    private void validateDuplicate(String factoryName, String lineName, Integer operationSeq) {
        if (factoryRoutingRepository.existsByFactoryNameAndLineNameAndOperationSeq(
                factoryName,
                lineName,
                operationSeq
        )) {
            throw new BusinessException(ErrorCode.ROUTING_DUPLICATE);
        }
    }

    private void validateDuplicateExceptSelf(
            String factoryName,
            String lineName,
            Integer operationSeq,
            Integer routingId
    ) {
        if (factoryRoutingRepository.existsByFactoryNameAndLineNameAndOperationSeqAndRoutingIdNot(
                factoryName,
                lineName,
                operationSeq,
                routingId
        )) {
            throw new BusinessException(ErrorCode.ROUTING_DUPLICATE);
        }
    }

    private void validateNoReference(FactoryRouting routing) {
        if (workOrderRepository.existsByRouting(routing) || productionExecutionRepository.existsByRouting(routing)) {
            throw new BusinessException(ErrorCode.ROUTING_HAS_REFERENCE);
        }
    }

    private FactoryRouting saveRouting(FactoryRouting routing) {
        try {
            return factoryRoutingRepository.save(routing);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ROUTING_DUPLICATE);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimRequired(String value) {
        return value.trim();
    }
}

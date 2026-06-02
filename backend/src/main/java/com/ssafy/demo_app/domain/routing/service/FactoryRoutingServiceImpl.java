package com.ssafy.demo_app.domain.routing.service;

import com.ssafy.demo_app.api.routing.dto.FactoryRoutingRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingTreeResponse;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.domain.routing.repository.FactoryRoutingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<FactoryRoutingResponse> getRoutings(String factoryName, String lineName) {
        List<FactoryRouting> routings;
        if (hasText(factoryName) && hasText(lineName)) {
            routings = factoryRoutingRepository.findByFactoryNameAndLineNameOrderByOperationSeqAsc(
                    factoryName.trim(),
                    lineName.trim()
            );
        } else if (hasText(factoryName)) {
            routings = factoryRoutingRepository.findByFactoryNameOrderByLineNameAscOperationSeqAsc(factoryName.trim());
        } else {
            routings = factoryRoutingRepository.findAllByOrderByFactoryNameAscLineNameAscOperationSeqAsc();
        }

        return routings.stream()
                .map(FactoryRoutingResponse::from)
                .toList();
    }

    @Override
    public FactoryRoutingResponse getRouting(Integer routingId) {
        return FactoryRoutingResponse.from(findRouting(routingId));
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

        return FactoryRoutingResponse.from(factoryRoutingRepository.save(routing));
    }

    @Override
    @Transactional
    public FactoryRoutingResponse updateRouting(Integer routingId, FactoryRoutingRequest request) {
        FactoryRouting routing = findRouting(routingId);
        String factoryName = trimRequired(request.getFactoryName());
        String lineName = trimRequired(request.getLineName());
        String operationName = trimRequired(request.getOperationName());

        validateDuplicateExceptSelf(factoryName, lineName, request.getOperationSeq(), routingId);

        routing.setFactoryName(factoryName);
        routing.setLineName(lineName);
        routing.setOperationSeq(request.getOperationSeq());
        routing.setOperationName(operationName);

        return FactoryRoutingResponse.from(factoryRoutingRepository.save(routing));
    }

    @Override
    @Transactional
    public void deleteRouting(Integer routingId) {
        FactoryRouting routing = findRouting(routingId);
        if (workOrderRepository.existsByRouting(routing)) {
            throw new BusinessException(ErrorCode.ROUTING_HAS_WORK_ORDER);
        }
        factoryRoutingRepository.delete(routing);
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
                .map(FactoryRoutingResponse::from)
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimRequired(String value) {
        return value.trim();
    }
}

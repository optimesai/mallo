package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionCreateRequest;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;

import java.util.List;

public interface ProductionExecutionService {
    ProductionExecutionResponse createExecution(Integer workerId, ProductionExecutionCreateRequest request);
    List<ProductionExecutionResponse> getExecutions(String orderKey);
    ProductionExecutionResponse getExecution(Integer executionId);
    void deleteExecution(Integer executionId);
}

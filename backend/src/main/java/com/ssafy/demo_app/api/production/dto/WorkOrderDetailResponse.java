package com.ssafy.demo_app.api.production.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WorkOrderDetailResponse {
    private WorkOrderResponse workOrder;
    private List<WorkOrderMaterialRequirementResponse> materialRequirements;
    private List<ProductionExecutionResponse> executions;
    private List<ProductionIssueHistoryResponse> issueHistories;
}

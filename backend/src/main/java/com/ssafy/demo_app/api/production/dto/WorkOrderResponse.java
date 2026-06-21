package com.ssafy.demo_app.api.production.dto;

import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "작업 지시 응답 객체")
public class WorkOrderResponse {

    private Integer orderId;
    private String orderNo;
    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private Integer routingId;
    private String factoryName;
    private String lineName;
    private Integer operationSeq;
    private String operationName;
    private Integer currentOperationRoutingId;
    private Integer currentOperationSeq;
    private String currentOperationName;
    private Integer targetQty;
    private String bomVersion;
    private String status;
    private LocalDate planDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalGoodQty;
    private Integer totalDefectQty;
    private Integer totalExecutedQty;
    private Integer totalManHoursMinutes;
    private Double progressRate;
    private Boolean canIssueMaterials;
    private Boolean canStart;
    private Boolean canHold;
    private Boolean canClose;
    private Boolean canRegisterExecution;
    private Boolean canCancelIssue;
    private Boolean canDeleteExecution;
    private Boolean canUpdate;
    private Boolean canDelete;

    public static WorkOrderResponse from(
            WorkOrder workOrder,
            WorkOrderExecutionSummary summary,
            boolean canCancelIssue,
            boolean canDeleteExecution
    ) {
        return from(workOrder, summary, canCancelIssue, canDeleteExecution, null);
    }

    public static WorkOrderResponse from(
            WorkOrder workOrder,
            WorkOrderExecutionSummary summary,
            boolean canCancelIssue,
            boolean canDeleteExecution,
            WorkOrderOperationProgressResponse currentOperation
    ) {
        int totalGoodQty = summary.totalGoodQty();
        int totalDefectQty = summary.totalDefectQty();
        int totalExecutedQty = totalGoodQty + totalDefectQty;
        double progressRate = workOrder.getTargetQty() == null || workOrder.getTargetQty() == 0
                ? 0.0
                : Math.round((totalGoodQty * 10000.0) / workOrder.getTargetQty()) / 100.0;
        WorkOrder.OrderStatus status = workOrder.getStatus();

        return WorkOrderResponse.builder()
                .orderId(workOrder.getOrderId())
                .orderNo(workOrder.getOrderNo())
                .itemId(workOrder.getItem().getItemId())
                .itemCode(workOrder.getItem().getItemCode())
                .itemName(workOrder.getItem().getItemName())
                .itemType(workOrder.getItem().getItemType().name())
                .routingId(workOrder.getRouting().getRoutingId())
                .factoryName(workOrder.getRouting().getFactoryName())
                .lineName(workOrder.getRouting().getLineName())
                .operationSeq(workOrder.getRouting().getOperationSeq())
                .operationName(workOrder.getRouting().getOperationName())
                .currentOperationRoutingId(currentOperation != null ? currentOperation.getRoutingId() : workOrder.getRouting().getRoutingId())
                .currentOperationSeq(currentOperation != null ? currentOperation.getOperationSeq() : workOrder.getRouting().getOperationSeq())
                .currentOperationName(currentOperation != null ? currentOperation.getOperationName() : workOrder.getRouting().getOperationName())
                .targetQty(workOrder.getTargetQty())
                .bomVersion(workOrder.getBomVersion())
                .status(status.name())
                .planDate(workOrder.getPlanDate())
                .createdAt(workOrder.getCreatedAt())
                .updatedAt(workOrder.getUpdatedAt())
                .totalGoodQty(totalGoodQty)
                .totalDefectQty(totalDefectQty)
                .totalExecutedQty(totalExecutedQty)
                .totalManHoursMinutes(summary.totalManHoursMinutes())
                .progressRate(progressRate)
                .canIssueMaterials(status == WorkOrder.OrderStatus.READY)
                .canStart(status == WorkOrder.OrderStatus.READY)
                .canHold(status == WorkOrder.OrderStatus.RUN)
                .canClose(status == WorkOrder.OrderStatus.RUN || status == WorkOrder.OrderStatus.HOLD)
                .canRegisterExecution(status == WorkOrder.OrderStatus.RUN)
                .canCancelIssue(canCancelIssue)
                .canDeleteExecution(canDeleteExecution)
                .canUpdate(status == WorkOrder.OrderStatus.READY)
                .canDelete(status == WorkOrder.OrderStatus.READY)
                .build();
    }
}

package com.ssafy.demo_app.api.production.dto;

import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "생산 실적 응답 객체")
public class ProductionExecutionResponse {

    private Integer executionId;
    private Integer orderId;
    private String orderNo;
    private Integer routingId;
    private String factoryName;
    private String lineName;
    private Integer operationSeq;
    private String operationName;
    private Integer goodQty;
    private Integer defectQty;
    private String defectType;
    private String defectReason;
    private Boolean reworkable;
    private Integer executedQty;
    private Integer workerId;
    private String workerEmployeeNo;
    private String workerName;
    private Integer manHoursMinutes;
    private Boolean canDelete;
    private LocalDateTime createdAt;

    public static ProductionExecutionResponse from(ProductionExecution execution) {
        return ProductionExecutionResponse.builder()
                .executionId(execution.getExecutionId())
                .orderId(execution.getOrder().getOrderId())
                .orderNo(execution.getOrder().getOrderNo())
                .routingId(execution.getRouting() != null ? execution.getRouting().getRoutingId() : null)
                .factoryName(execution.getRouting() != null ? execution.getRouting().getFactoryName() : null)
                .lineName(execution.getRouting() != null ? execution.getRouting().getLineName() : null)
                .operationSeq(execution.getRouting() != null ? execution.getRouting().getOperationSeq() : null)
                .operationName(execution.getRouting() != null ? execution.getRouting().getOperationName() : null)
                .goodQty(execution.getGoodQty())
                .defectQty(execution.getDefectQty())
                .defectType(execution.getDefectType())
                .defectReason(execution.getDefectReason())
                .reworkable(execution.getReworkable())
                .executedQty(execution.getGoodQty() + execution.getDefectQty())
                .workerId(execution.getWorker() != null ? execution.getWorker().getUserId() : null)
                .workerEmployeeNo(execution.getWorker() != null ? execution.getWorker().getEmployeeNo() : null)
                .workerName(execution.getWorker() != null ? execution.getWorker().getUserName() : null)
                .manHoursMinutes(execution.getManHoursMinutes())
                .canDelete(execution.getOrder().getStatus() != WorkOrder.OrderStatus.CLOSE)
                .createdAt(execution.getCreatedAt())
                .build();
    }
}

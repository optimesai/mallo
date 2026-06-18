package com.ssafy.demo_app.api.production.dto;

import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductionIssueHistoryResponse {

    private Integer transactionId;
    private Integer itemId;
    private String itemCode;
    private String itemName;
    private Integer locationId;
    private String locationCode;
    private String transactionType;
    private Integer quantity;
    private String reasonDesc;
    private Integer workerId;
    private String workerEmployeeNo;
    private String workerName;
    private Integer executionId;
    private Integer originalTransactionId;
    private LocalDateTime createdAt;

    public static ProductionIssueHistoryResponse from(InventoryTransactionHistory history) {
        return ProductionIssueHistoryResponse.builder()
                .transactionId(history.getTransactionId())
                .itemId(history.getItem().getItemId())
                .itemCode(history.getItem().getItemCode())
                .itemName(history.getItem().getItemName())
                .locationId(history.getLocation().getLocationId())
                .locationCode(history.getLocation().getLocationCode())
                .transactionType(history.getTransactionType().name())
                .quantity(history.getQuantity())
                .reasonDesc(history.getReasonDesc())
                .workerId(history.getWorker() != null ? history.getWorker().getUserId() : null)
                .workerEmployeeNo(history.getWorker() != null ? history.getWorker().getEmployeeNo() : null)
                .workerName(history.getWorker() != null ? history.getWorker().getUserName() : null)
                .executionId(history.getProductionExecution() != null ? history.getProductionExecution().getExecutionId() : null)
                .originalTransactionId(history.getOriginalTransaction() != null ? history.getOriginalTransaction().getTransactionId() : null)
                .createdAt(history.getCreatedAt())
                .build();
    }
}

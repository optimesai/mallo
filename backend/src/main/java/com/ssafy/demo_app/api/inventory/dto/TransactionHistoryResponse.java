package com.ssafy.demo_app.api.inventory.dto;

import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionHistoryResponse {

    private Integer transactionId;
    private String itemCode;
    private String itemName;
    private String locationCode;
    private String transactionType;
    private Integer quantity;
    private String reasonDesc;
    private String workerName;
    private LocalDateTime createdAt;

    public static TransactionHistoryResponse from(InventoryTransactionHistory history) {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        response.setTransactionId(history.getTransactionId());
        response.setItemCode(history.getItem().getItemCode());
        response.setItemName(history.getItem().getItemName());
        response.setLocationCode(history.getLocation().getLocationCode());
        response.setTransactionType(history.getTransactionType().name());
        response.setQuantity(history.getQuantity());
        response.setReasonDesc(history.getReasonDesc());
        if (history.getWorker() != null) {
            response.setWorkerName(history.getWorker().getUserName());
        }
        response.setCreatedAt(history.getCreatedAt());
        return response;
    }
}

package com.ssafy.demo_app.api.item.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemReferenceResponse {

    private Integer itemId;
    private String itemCode;
    private long bomParentCount;
    private long bomChildCount;
    private long inventoryCount;
    private long inboundCount;
    private long transactionHistoryCount;
    private long workOrderCount;
    private long shippingCount;
    private boolean hasReferences;
    private boolean deletable;

    public void calculateFlags() {
        hasReferences = bomParentCount > 0
                || bomChildCount > 0
                || inventoryCount > 0
                || inboundCount > 0
                || transactionHistoryCount > 0
                || workOrderCount > 0
                || shippingCount > 0;
        deletable = !hasReferences;
    }
}

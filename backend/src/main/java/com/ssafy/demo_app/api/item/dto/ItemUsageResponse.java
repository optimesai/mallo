package com.ssafy.demo_app.api.item.dto;

import com.ssafy.demo_app.api.bom.dto.BomResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemUsageResponse {

    private Integer itemId;
    private String itemCode;
    private int currentQtyTotal;
    private List<BomResponse> asParentBoms;
    private List<BomResponse> asChildBoms;
    private List<WorkOrderResponse> workOrders;
    private long shippingCount;
    private List<TransactionHistoryResponse> recentTransactions;
}

package com.ssafy.demo_app.api.production.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class WorkOrderMaterialRequirementResponse {
    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    private BigDecimal bomQuantity;
    private Integer requiredQty;
    private Integer issuedQty;
    private Integer availableQty;
}

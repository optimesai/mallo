package com.ssafy.demo_app.api.production.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkOrderMaterialRequirementResponse {
    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    private Integer bomQuantity;
    private Integer requiredQty;
    private Integer issuedQty;
    private Integer availableQty;
}

package com.ssafy.demo_app.api.inventory.dto;

import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CurrentInventoryResponse {

    private Integer inventoryId;
    private String itemCode;
    private String itemName;
    private String locationCode;
    private String warehouseName;
    private Integer currentQty;
    private LocalDateTime updatedAt;

    public static CurrentInventoryResponse from(CurrentInventory inventory) {
        CurrentInventoryResponse response = new CurrentInventoryResponse();
        response.setInventoryId(inventory.getInventoryId());
        response.setItemCode(inventory.getItem().getItemCode());
        response.setItemName(inventory.getItem().getItemName());
        response.setLocationCode(inventory.getLocation().getLocationCode());
        response.setWarehouseName(inventory.getLocation().getWarehouseName());
        response.setCurrentQty(inventory.getCurrentQty());
        response.setUpdatedAt(inventory.getUpdatedAt());
        return response;
    }
}

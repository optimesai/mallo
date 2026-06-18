package com.ssafy.demo_app.api.item.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "품목 마스터 응답 객체")
public class ItemResponse {

    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String spec;
    private ItemMaster.Unit unit;
    private ItemMaster.ItemType itemType;
    private Integer safetyStock;
    private ItemMaster.ItemStatus itemStatus;
    private LocalDateTime createdAt;

    public static ItemResponse from(ItemMaster item) {
        ItemResponse response = new ItemResponse();
        response.setItemId(item.getItemId());
        response.setItemCode(item.getItemCode());
        response.setItemName(item.getItemName());
        response.setSpec(item.getSpec());
        response.setUnit(item.getUnit());
        response.setItemType(item.getItemType());
        response.setSafetyStock(item.getSafetyStock());
        response.setItemStatus(item.getItemStatus());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }
}

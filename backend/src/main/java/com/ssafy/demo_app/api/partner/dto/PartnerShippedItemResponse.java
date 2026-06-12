package com.ssafy.demo_app.api.partner.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PartnerShippedItemResponse {

    private String itemCode;
    private String itemName;
    private ItemMaster.ItemType itemType;
    private ItemMaster.Unit unit;
    private long totalShippingQty;
    private long shippingCount;
    private LocalDateTime lastShippingAt;
}

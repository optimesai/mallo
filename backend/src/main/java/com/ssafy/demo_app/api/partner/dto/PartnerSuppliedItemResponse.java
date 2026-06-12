package com.ssafy.demo_app.api.partner.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PartnerSuppliedItemResponse {

    private String itemCode;
    private String itemName;
    private ItemMaster.ItemType itemType;
    private ItemMaster.Unit unit;
    private long totalInboundQty;
    private long inboundCount;
    private LocalDate lastInboundDate;
}

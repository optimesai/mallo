package com.ssafy.demo_app.domain.bom.service;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;

public record BomRequirement(
        ItemMaster item,
        Integer bomQuantity,
        Integer requiredQty
) {
}

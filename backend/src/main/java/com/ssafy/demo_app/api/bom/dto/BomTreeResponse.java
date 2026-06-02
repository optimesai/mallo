package com.ssafy.demo_app.api.bom.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "BOM 하향 트리 응답 객체")
public class BomTreeResponse {

    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    private BigDecimal quantity;
    private String bomVersion;
    private List<BomTreeResponse> children;

    public static BomTreeResponse root(ItemMaster item, List<BomTreeResponse> children) {
        return of(item, BigDecimal.ONE, null, children);
    }

    public static BomTreeResponse of(
            ItemMaster item,
            BigDecimal quantity,
            String bomVersion,
            List<BomTreeResponse> children
    ) {
        return new BomTreeResponse(
                item.getItemId(),
                item.getItemCode(),
                item.getItemName(),
                item.getItemType().name(),
                item.getUnit().name(),
                quantity,
                bomVersion,
                children
        );
    }
}

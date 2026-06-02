package com.ssafy.demo_app.api.bom.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "BOM 상향 역조회 트리 응답 객체")
public class BomReverseTreeResponse {

    private Integer itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    private BigDecimal quantity;
    private String bomVersion;
    private List<BomReverseTreeResponse> parents;

    public static BomReverseTreeResponse root(ItemMaster item, List<BomReverseTreeResponse> parents) {
        return of(item, BigDecimal.ONE, null, parents);
    }

    public static BomReverseTreeResponse of(
            ItemMaster item,
            BigDecimal quantity,
            String bomVersion,
            List<BomReverseTreeResponse> parents
    ) {
        return new BomReverseTreeResponse(
                item.getItemId(),
                item.getItemCode(),
                item.getItemName(),
                item.getItemType().name(),
                item.getUnit().name(),
                quantity,
                bomVersion,
                parents
        );
    }
}

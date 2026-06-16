package com.ssafy.demo_app.api.bom.dto;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "BOM 역조회 응답 객체")
public class BomReverseResponse {

    private Integer bomId;
    private Integer childItemId;
    private String childItemCode;
    private String childItemName;
    private String childItemType;
    private Integer parentItemId;
    private String parentItemCode;
    private String parentItemName;
    private String parentItemType;
    private Integer quantity;
    private String bomVersion;

    public static BomReverseResponse from(BomStructure bom) {
        return new BomReverseResponse(
                bom.getBomId(),
                bom.getChildItem().getItemId(),
                bom.getChildItem().getItemCode(),
                bom.getChildItem().getItemName(),
                bom.getChildItem().getItemType().name(),
                bom.getParentItem().getItemId(),
                bom.getParentItem().getItemCode(),
                bom.getParentItem().getItemName(),
                bom.getParentItem().getItemType().name(),
                bom.getQuantity(),
                bom.getBomVersion()
        );
    }
}

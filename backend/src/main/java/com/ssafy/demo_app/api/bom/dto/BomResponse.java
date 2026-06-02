package com.ssafy.demo_app.api.bom.dto;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "BOM 응답 객체")
public class BomResponse {

    private Integer bomId;
    private Integer parentItemId;
    private String parentItemCode;
    private String parentItemName;
    private String parentItemType;
    private Integer childItemId;
    private String childItemCode;
    private String childItemName;
    private String childItemType;
    private String childUnit;
    private BigDecimal quantity;
    private String bomVersion;
    private LocalDateTime createdAt;

    public static BomResponse from(BomStructure bom) {
        return new BomResponse(
                bom.getBomId(),
                bom.getParentItem().getItemId(),
                bom.getParentItem().getItemCode(),
                bom.getParentItem().getItemName(),
                bom.getParentItem().getItemType().name(),
                bom.getChildItem().getItemId(),
                bom.getChildItem().getItemCode(),
                bom.getChildItem().getItemName(),
                bom.getChildItem().getItemType().name(),
                bom.getChildItem().getUnit().name(),
                bom.getQuantity(),
                bom.getBomVersion(),
                bom.getCreatedAt()
        );
    }
}

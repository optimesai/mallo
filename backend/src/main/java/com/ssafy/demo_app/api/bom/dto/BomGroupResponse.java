package com.ssafy.demo_app.api.bom.dto;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class BomGroupResponse {

    private Integer parentItemId;
    private String parentItemCode;
    private String parentItemName;
    private String parentItemType;
    private String bomVersion;
    private Integer childCount;
    private String bomStatus;
    private LocalDateTime createdAt;

    public BomGroupResponse(
            Integer parentItemId,
            String parentItemCode,
            String parentItemName,
            ItemMaster.ItemType parentItemType,
            String bomVersion,
            Long childCount,
            Long activeCount,
            LocalDateTime createdAt
    ) {
        this.parentItemId = parentItemId;
        this.parentItemCode = parentItemCode;
        this.parentItemName = parentItemName;
        this.parentItemType = parentItemType.name();
        this.bomVersion = bomVersion;
        this.childCount = childCount.intValue();
        this.bomStatus = activeCount > 0 ? BomStructure.BomStatus.ACTIVE.name() : BomStructure.BomStatus.INACTIVE.name();
        this.createdAt = createdAt;
    }

    public static BomGroupResponse from(List<BomStructure> boms) {
        BomStructure first = boms.get(0);
        boolean hasActive = boms.stream()
                .anyMatch(bom -> bom.getBomStatus() == BomStructure.BomStatus.ACTIVE);
        return new BomGroupResponse(
                first.getParentItem().getItemId(),
                first.getParentItem().getItemCode(),
                first.getParentItem().getItemName(),
                first.getParentItem().getItemType().name(),
                first.getBomVersion(),
                boms.size(),
                hasActive ? BomStructure.BomStatus.ACTIVE.name() : BomStructure.BomStatus.INACTIVE.name(),
                first.getCreatedAt()
        );
    }
}

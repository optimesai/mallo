package com.ssafy.demo_app.api.item.dto;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "품목 마스터 수정 요청 객체")
public class ItemUpdateRequest {

    @Schema(description = "품목명", example = "고탄소 탄소강판")
    @NotBlank(message = "품목명은 필수입니다.")
    @Size(max = 100, message = "품목명은 100자 이하여야 합니다.")
    private String itemName;

    @Schema(description = "규격 및 사이즈", example = "2.0T * 1219 * 2438")
    @Size(max = 100, message = "규격은 100자 이하여야 합니다.")
    private String spec;

    @Schema(description = "기본 단위", example = "kg")
    @NotNull(message = "기본 단위는 필수입니다.")
    private ItemMaster.Unit unit;

    @Schema(description = "품목 분류", example = "RAW")
    @NotNull(message = "품목 분류는 필수입니다.")
    private ItemMaster.ItemType itemType;

    @Schema(description = "안전 재고량", example = "5000")
    @NotNull(message = "안전 재고량은 필수입니다.")
    @Min(value = 0, message = "안전 재고량은 0 이상이어야 합니다.")
    private Integer safetyStock;

    @Schema(description = "참조 중 품목 분류 변경 경고 확인 여부", example = "false")
    private Boolean confirmReferenceWarning = false;
}

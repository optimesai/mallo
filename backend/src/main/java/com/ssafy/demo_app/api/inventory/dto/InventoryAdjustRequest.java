package com.ssafy.demo_app.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "재고 조정 요청 객체")
public class InventoryAdjustRequest {

    @Schema(description = "품목 코드", example = "RM-STEEL-01")
    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @Schema(description = "로케이션 코드", example = "WH01-RACK-A1")
    @NotBlank(message = "로케이션 코드는 필수입니다.")
    private String locationCode;

    @Schema(description = "조정 수량 (양수: 증가, 음수: 감소)", example = "5")
    @NotNull(message = "조정 수량은 필수입니다.")
    @Min(value = 1, message = "조정 수량은 1 이상이어야 합니다.")
    private Integer adjustQty;

    @Schema(description = "조정 유형 (INCREASE / DECREASE)", example = "INCREASE")
    @NotBlank(message = "조정 유형은 필수입니다.")
    private String adjustType;

    @Schema(description = "조정 사유", example = "실사 결과 실제 재고 +5 확인")
    @NotBlank(message = "조정 사유는 필수입니다.")
    private String reasonDesc;
}

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
@Schema(description = "재고 폐기 요청 객체")
public class InventoryScrapRequest {

    @Schema(description = "품목 코드", example = "RM-STEEL-01")
    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @Schema(description = "로케이션 코드", example = "WH01-RACK-A1")
    @NotBlank(message = "로케이션 코드는 필수입니다.")
    private String locationCode;

    @Schema(description = "폐기 수량", example = "3")
    @NotNull(message = "폐기 수량은 필수입니다.")
    @Min(value = 1, message = "폐기 수량은 1 이상이어야 합니다.")
    private Integer scrapQty;

    @Schema(description = "폐기 사유", example = "불량품 폐기 처리")
    @NotBlank(message = "폐기 사유는 필수입니다.")
    private String reasonDesc;
}

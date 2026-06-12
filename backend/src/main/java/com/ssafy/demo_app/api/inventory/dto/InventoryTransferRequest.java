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
@Schema(description = "로케이션 간 재고 이동 요청 객체")
public class InventoryTransferRequest {

    @Schema(description = "품목 코드", example = "RM-STEEL-01")
    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @Schema(description = "출발 로케이션 코드", example = "WH01-RACK-A1")
    @NotBlank(message = "출발 로케이션 코드는 필수입니다.")
    private String fromLocationCode;

    @Schema(description = "도착 로케이션 코드", example = "WH01-RACK-B2")
    @NotBlank(message = "도착 로케이션 코드는 필수입니다.")
    private String toLocationCode;

    @Schema(description = "이동 수량", example = "10")
    @NotNull(message = "이동 수량은 필수입니다.")
    @Min(value = 1, message = "이동 수량은 1 이상이어야 합니다.")
    private Integer transferQty;

    @Schema(description = "이동 사유", example = "생산 라인 인근 창고로 이관")
    @NotBlank(message = "이동 사유는 필수입니다.")
    private String reasonDesc;
}

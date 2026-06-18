package com.ssafy.demo_app.api.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "작업 지시 생성 요청 객체")
public class WorkOrderCreateRequest {

    @Schema(description = "생산 품목 코드", example = "FP-SMART-BOX")
    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @Schema(description = "라우팅 ID", example = "1")
    @NotNull(message = "라우팅 ID는 필수입니다.")
    private Integer routingId;

    @Schema(description = "생산 목표 수량", example = "100")
    @NotNull(message = "생산 목표 수량은 필수입니다.")
    @Min(value = 1, message = "생산 목표 수량은 1 이상이어야 합니다.")
    private Integer targetQty;

    @Schema(description = "BOM 버전", example = "v1.0")
    private String bomVersion;

    @Schema(description = "생산 계획일", example = "2026-06-03")
    @NotNull(message = "생산 계획일은 필수입니다.")
    private LocalDate planDate;
}

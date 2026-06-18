package com.ssafy.demo_app.api.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공장/라인/공정 라우팅 생성/수정 요청 객체")
public class FactoryRoutingRequest {

    @Schema(description = "공장명", example = "창원제1공장")
    @NotBlank(message = "공장명은 필수입니다.")
    @Size(max = 50, message = "공장명은 50자 이하여야 합니다.")
    private String factoryName;

    @Schema(description = "생산 라인명", example = "A라인")
    @NotBlank(message = "생산 라인명은 필수입니다.")
    @Size(max = 50, message = "생산 라인명은 50자 이하여야 합니다.")
    private String lineName;

    @Schema(description = "공정 순서", example = "1")
    @NotNull(message = "공정 순서는 필수입니다.")
    @Min(value = 1, message = "공정 순서는 1 이상이어야 합니다.")
    private Integer operationSeq;

    @Schema(description = "세부 공정명", example = "SMD 표면실장 공정")
    @NotBlank(message = "세부 공정명은 필수입니다.")
    @Size(max = 50, message = "세부 공정명은 50자 이하여야 합니다.")
    private String operationName;
}

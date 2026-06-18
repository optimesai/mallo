package com.ssafy.demo_app.api.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
@Schema(description = "BOM 생성/수정 요청 객체")
public class BomRequest {

    @Schema(description = "부모 품목 ID", example = "7")
    @NotNull(message = "부모 품목 ID는 필수입니다.")
    private Integer parentItemId;

    @Schema(description = "자식 품목 ID", example = "5")
    @NotNull(message = "자식 품목 ID는 필수입니다.")
    private Integer childItemId;

    @Schema(description = "부모 1단위 생산에 필요한 자식 품목 정수 소요량", example = "1")
    @NotNull(message = "소요량은 필수입니다.")
    @Min(value = 1, message = "소요량은 1 이상이어야 합니다.")
    private Integer quantity;

    @Schema(description = "BOM 버전", example = "v1.0")
    @Size(max = 20, message = "BOM 버전은 20자 이하여야 합니다.")
    private String bomVersion;
}

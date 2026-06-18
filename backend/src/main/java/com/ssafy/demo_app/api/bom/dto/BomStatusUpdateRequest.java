package com.ssafy.demo_app.api.bom.dto;

import com.ssafy.demo_app.domain.bom.entity.BomStructure;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "BOM 상태 변경 요청 객체")
public class BomStatusUpdateRequest {

    @Schema(description = "BOM 상태", example = "ACTIVE")
    @NotNull(message = "BOM 상태는 필수입니다.")
    private BomStructure.BomStatus bomStatus;
}

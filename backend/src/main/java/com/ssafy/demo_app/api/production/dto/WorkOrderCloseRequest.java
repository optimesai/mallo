package com.ssafy.demo_app.api.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "작업 지시 마감 요청 객체")
public class WorkOrderCloseRequest {

    @Schema(description = "목표 미달 마감 허용 여부", example = "false")
    private Boolean allowUnderTargetClose;
}

package com.ssafy.demo_app.api.routing.dto;

import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "라우팅 상태 변경 요청 객체")
public class FactoryRoutingStatusUpdateRequest {

    @Schema(description = "라우팅 상태", example = "INACTIVE")
    @NotNull(message = "라우팅 상태는 필수입니다.")
    private FactoryRouting.RoutingStatus routingStatus;
}

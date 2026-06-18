package com.ssafy.demo_app.api.production.dto;

import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "작업 지시 상태 변경 요청 객체")
public class WorkOrderStatusUpdateRequest {

    @Schema(description = "변경할 작업 지시 상태", example = "HOLD")
    @NotNull(message = "상태는 필수입니다.")
    private WorkOrder.OrderStatus status;
}

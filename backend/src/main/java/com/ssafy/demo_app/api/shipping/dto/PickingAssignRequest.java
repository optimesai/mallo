package com.ssafy.demo_app.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "피킹 지시 및 차량 배정 요청 객체")
public class PickingAssignRequest {

    @Schema(description = "배정 차량 번호", example = "경기 88 가 1234")
    @NotBlank(message = "차량 번호는 필수입니다.")
    private String vehicleNo;
}

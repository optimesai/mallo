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
@Schema(description = "출하 취소 요청 객체")
public class CancelShippingRequest {

    @Schema(description = "취소 사유", example = "고객사 주문 취소")
    @NotBlank(message = "취소 사유는 필수입니다.")
    private String cancelReason;
}

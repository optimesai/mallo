package com.ssafy.demo_app.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
@Schema(description = "부분 출하 요청 객체")
public class PartialShipRequest {

    @Schema(description = "출하 완료 수량", example = "30")
    @NotNull(message = "출하 수량은 필수입니다.")
    @Min(value = 1, message = "출하 수량은 1 이상이어야 합니다.")
    private Integer shipQty;

    @Schema(description = "운송사", example = "대한통운")
    private String carrier;

    @Schema(description = "송장 번호", example = "1234567890")
    private String trackingNo;

    @Schema(description = "배송 예정일", example = "2026-06-10")
    private LocalDate estimatedDelivery;
}

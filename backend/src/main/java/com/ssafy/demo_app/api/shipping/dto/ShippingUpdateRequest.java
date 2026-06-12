package com.ssafy.demo_app.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출하 지시 수정 요청 객체")
public class ShippingUpdateRequest {

    @Schema(description = "요청 수량", example = "80")
    private Integer requestQty;

    @Schema(description = "출하 유형 (SALE, TRANSFER, RETURN)", example = "SALE")
    private String shippingType;

    @Schema(description = "운송사", example = "대한통운")
    private String carrier;

    @Schema(description = "송장 번호", example = "1234567890")
    private String trackingNo;

    @Schema(description = "배송 예정일", example = "2026-06-10")
    private LocalDate estimatedDelivery;
}

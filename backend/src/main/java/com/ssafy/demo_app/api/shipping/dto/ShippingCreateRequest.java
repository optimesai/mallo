package com.ssafy.demo_app.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출하 지시 등록 요청 객체")
public class ShippingCreateRequest {

    @Schema(description = "출하 지시 번호", example = "SH-2026-001")
    @NotBlank(message = "출하 지시 번호는 필수입니다.")
    private String shippingNo;

    @Schema(description = "고객사 코드", example = "CUS-HYUNDAI-M")
    @NotBlank(message = "고객사 코드는 필수입니다.")
    private String partnerCode;

    @Schema(description = "품목 코드", example = "FP-SMART-BOX")
    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @Schema(description = "요청 수량", example = "100")
    @NotNull(message = "요청 수량은 필수입니다.")
    @Min(value = 1, message = "요청 수량은 1 이상이어야 합니다.")
    private Integer requestQty;
}

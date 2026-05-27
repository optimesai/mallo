package com.ssafy.demo_app.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "입고 예정 등록 요청 객체")
public class InboundCreateRequest {

    @Schema(description = "품목 코드", example = "RM-STEEL-01")
    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @Schema(description = "거래처 코드", example = "SUP-POSCO-01")
    @NotBlank(message = "거래처 코드는 필수입니다.")
    private String partnerCode;

    @Schema(description = "입고 등록 시점 지정 로케이션 코드", example = "WH01-RACK-A1")
    @NotBlank(message = "로케이션 코드는 필수입니다.")
    private String locationCode;

    @Schema(description = "예정 입고 수량", example = "150")
    @NotNull(message = "입고 수량은 필수입니다.")
    @Min(value = 1, message = "입고 수량은 1 이상이어야 합니다.")
    private Integer inboundQty;

    @Schema(description = "입고 예정 일자", example = "2026-05-27")
    @NotNull(message = "입고 예정일은 필수입니다.")
    private LocalDate inboundDate;
}

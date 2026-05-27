package com.ssafy.demo_app.api.inventory.dto;

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
public class InboundCreateRequest {

    @NotBlank(message = "품목 코드는 필수입니다.")
    private String itemCode;

    @NotBlank(message = "거래처 코드는 필수입니다.")
    private String partnerCode;

    @NotBlank(message = "로케이션 코드는 필수입니다.")
    private String locationCode;

    @NotNull(message = "입고 수량은 필수입니다.")
    @Min(value = 1, message = "입고 수량은 1 이상이어야 합니다.")
    private Integer inboundQty;

    @NotNull(message = "입고 예정일은 필수입니다.")
    private LocalDate inboundDate;
}

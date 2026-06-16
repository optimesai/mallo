package com.ssafy.demo_app.api.bom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BomBulkLineRequest {

    @NotNull(message = "자식 품목 ID는 필수입니다.")
    private Integer childItemId;

    @NotNull(message = "소요량은 필수입니다.")
    @Min(value = 1, message = "소요량은 1 이상이어야 합니다.")
    private Integer quantity;
}

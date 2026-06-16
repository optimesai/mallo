package com.ssafy.demo_app.api.bom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BomBulkRequest {

    @NotNull(message = "부모 품목 ID는 필수입니다.")
    private Integer parentItemId;

    @Size(max = 20, message = "BOM 버전은 20자 이하여야 합니다.")
    private String bomVersion;

    @Valid
    @NotEmpty(message = "구성 품목은 1개 이상이어야 합니다.")
    private List<BomBulkLineRequest> lines;
}

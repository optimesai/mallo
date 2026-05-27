package com.ssafy.demo_app.api.inventory.dto;

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
@Schema(description = "재고 적재 요청 객체")
public class InventoryStackRequest {

    @Schema(description = "자재를 실제 적재 및 바인딩할 대상 로케이션 코드", example = "WH01-RACK-A2")
    @NotBlank(message = "적재할 로케이션 코드는 필수입니다.")
    private String targetLocationCode;
}

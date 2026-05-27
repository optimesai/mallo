package com.ssafy.demo_app.api.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStackRequest {

    @NotBlank(message = "적재할 로케이션 코드는 필수입니다.")
    private String targetLocationCode;
}

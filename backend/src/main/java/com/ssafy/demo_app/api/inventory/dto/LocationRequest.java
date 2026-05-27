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
public class LocationRequest {

    @NotBlank(message = "로케이션 코드는 필수입니다.")
    private String locationCode;

    @NotBlank(message = "창고명은 필수입니다.")
    private String warehouseName;

    private String rackRow;
    private String rackColumn;
}

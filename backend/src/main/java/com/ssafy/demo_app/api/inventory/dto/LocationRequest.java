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
@Schema(description = "창고 로케이션 생성/수정 요청 객체")
public class LocationRequest {

    @Schema(description = "고유 로케이션 코드", example = "WH01-RACK-A3")
    @NotBlank(message = "로케이션 코드는 필수입니다.")
    private String locationCode;

    @Schema(description = "소속 창고명", example = "원자재 창고")
    @NotBlank(message = "창고명은 필수입니다.")
    private String warehouseName;

    @Schema(description = "렉(Rack) 열 정보", example = "A열")
    private String rackRow;

    @Schema(description = "렉(Rack) 단(Column) 정보", example = "3단")
    private String rackColumn;

    @Schema(description = "생산 입고 기본 로케이션 여부", example = "false")
    private Boolean productionReceiptDefault;

    public LocationRequest(String locationCode, String warehouseName, String rackRow, String rackColumn) {
        this.locationCode = locationCode;
        this.warehouseName = warehouseName;
        this.rackRow = rackRow;
        this.rackColumn = rackColumn;
    }
}

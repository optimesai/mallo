package com.ssafy.demo_app.api.production.dto;

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
@Schema(description = "생산 실적 등록 요청 객체")
public class ProductionExecutionCreateRequest {

    @Schema(description = "작업 지시 ID 또는 작업 지시 번호", example = "WO-20260603-001")
    @NotBlank(message = "작업 지시 ID 또는 작업 지시 번호는 필수입니다.")
    private String orderKey;

    @Schema(description = "실제 수행 라우팅 ID", example = "1")
    @NotNull(message = "실제 수행 라우팅 ID는 필수입니다.")
    private Integer routingId;

    @Schema(description = "양품 수량", example = "95")
    @NotNull(message = "양품 수량은 필수입니다.")
    @Min(value = 0, message = "양품 수량은 0 이상이어야 합니다.")
    private Integer goodQty;

    @Schema(description = "불량 수량", example = "5")
    @NotNull(message = "불량 수량은 필수입니다.")
    @Min(value = 0, message = "불량 수량은 0 이상이어야 합니다.")
    private Integer defectQty;

    @Schema(description = "불량 유형", example = "DIMENSION")
    private String defectType;

    @Schema(description = "불량 사유", example = "치수 오차")
    private String defectReason;

    @Schema(description = "재작업 가능 여부", example = "true")
    private Boolean reworkable;

    @Schema(description = "생산 입고 로케이션 코드", example = "PROD-IN-01")
    private String receiptLocationCode;

    @Schema(description = "총 소요 시간(분)", example = "480")
    @NotNull(message = "총 소요 시간은 필수입니다.")
    @Min(value = 1, message = "총 소요 시간은 1분 이상이어야 합니다.")
    private Integer manHoursMinutes;

    public ProductionExecutionCreateRequest(
            String orderKey,
            Integer routingId,
            Integer goodQty,
            Integer defectQty,
            Integer manHoursMinutes
    ) {
        this.orderKey = orderKey;
        this.routingId = routingId;
        this.goodQty = goodQty;
        this.defectQty = defectQty;
        this.manHoursMinutes = manHoursMinutes;
    }
}

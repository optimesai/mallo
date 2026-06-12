package com.ssafy.demo_app.api.shipping.dto;

import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출하 정보 응답 객체")
public class ShippingResponse {

    @Schema(description = "출하 ID", example = "1")
    private Integer shippingId;

    @Schema(description = "출하 지시 번호", example = "SH-2026-001")
    private String shippingNo;

    @Schema(description = "고객사 코드", example = "CUS-HYUNDAI-M")
    private String partnerCode;

    @Schema(description = "고객사명", example = "현대모비스 울산공장")
    private String partnerName;

    @Schema(description = "품목 코드", example = "FP-SMART-BOX")
    private String itemCode;

    @Schema(description = "품목명", example = "스마트 물류 제어 단말기")
    private String itemName;

    @Schema(description = "요청 수량", example = "100")
    private Integer requestQty;

    @Schema(description = "출하 완료 수량", example = "80")
    private Integer shippedQty;

    @Schema(description = "출하 유형", example = "SALE")
    private String shippingType;

    @Schema(description = "피킹 로케이션 코드", example = "WH03-RACK-C1")
    private String pickingLocationCode;

    @Schema(description = "배정 차량 번호", example = "서울 88 가 1234")
    private String vehicleNo;

    @Schema(description = "운송사", example = "대한통운")
    private String carrier;

    @Schema(description = "송장 번호", example = "1234567890")
    private String trackingNo;

    @Schema(description = "배송 예정일", example = "2026-06-10")
    private LocalDate estimatedDelivery;

    @Schema(description = "취소 사유", example = "고객사 주문 변경")
    private String cancelReason;

    @Schema(description = "출하 상태 (READY, PICKING, PACKING, INSPECTING, SHIPPED, PARTIALLY_SHIPPED, CANCELED)", example = "READY")
    private String status;

    @Schema(description = "담당 작업자명", example = "이창고")
    private String workerName;

    @Schema(description = "출하 완료 일시", example = "2026-05-27T22:00:00")
    private LocalDateTime shippedAt;

    public static ShippingResponse from(OutboundShipping shipping) {
        if (shipping == null) return null;
        return ShippingResponse.builder()
                .shippingId(shipping.getShippingId())
                .shippingNo(shipping.getShippingNo())
                .partnerCode(shipping.getPartner() != null ? shipping.getPartner().getPartnerCode() : null)
                .partnerName(shipping.getPartner() != null ? shipping.getPartner().getPartnerName() : null)
                .itemCode(shipping.getItem() != null ? shipping.getItem().getItemCode() : null)
                .itemName(shipping.getItem() != null ? shipping.getItem().getItemName() : null)
                .requestQty(shipping.getRequestQty())
                .shippedQty(shipping.getShippedQty())
                .shippingType(shipping.getShippingType() != null ? shipping.getShippingType().name() : null)
                .pickingLocationCode(shipping.getPickingLocation() != null ? shipping.getPickingLocation().getLocationCode() : null)
                .vehicleNo(shipping.getVehicleNo())
                .carrier(shipping.getCarrier())
                .trackingNo(shipping.getTrackingNo())
                .estimatedDelivery(shipping.getEstimatedDelivery())
                .cancelReason(shipping.getCancelReason())
                .status(shipping.getStatus() != null ? shipping.getStatus().name() : null)
                .workerName(shipping.getWorker() != null ? shipping.getWorker().getUserName() : null)
                .shippedAt(shipping.getShippedAt())
                .build();
    }
}

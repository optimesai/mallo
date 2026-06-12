package com.ssafy.demo_app.api.partner.dto;

import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "거래처 마스터 응답 객체")
public class PartnerResponse {

    private Integer partnerId;
    private String partnerCode;
    private String partnerName;
    private PartnerMaster.PartnerType partnerType;
    private PartnerMaster.PartnerStatus partnerStatus;
    private String businessNo;
    private String representative;
    private String contactPhone;
    private LocalDateTime createdAt;
    private long inboundCount;
    private long shippingCount;
    private long usageCount;

    public static PartnerResponse from(PartnerMaster partner) {
        PartnerResponse response = new PartnerResponse();
        response.setPartnerId(partner.getPartnerId());
        response.setPartnerCode(partner.getPartnerCode());
        response.setPartnerName(partner.getPartnerName());
        response.setPartnerType(partner.getPartnerType());
        response.setPartnerStatus(partner.getPartnerStatus());
        response.setBusinessNo(partner.getBusinessNo());
        response.setRepresentative(partner.getRepresentative());
        response.setContactPhone(partner.getContactPhone());
        response.setCreatedAt(partner.getCreatedAt());
        return response;
    }
}

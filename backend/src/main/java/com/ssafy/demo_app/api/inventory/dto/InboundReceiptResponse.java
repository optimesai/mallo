package com.ssafy.demo_app.api.inventory.dto;

import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class InboundReceiptResponse {

    private Integer inboundId;
    private String itemCode;
    private String itemName;
    private String partnerCode;
    private String partnerName;
    private String locationCode;
    private Integer inboundQty;
    private LocalDate inboundDate;
    private String status;
    private String workerName;
    private LocalDateTime createdAt;

    public static InboundReceiptResponse from(InboundReceipt inboundReceipt) {
        InboundReceiptResponse response = new InboundReceiptResponse();
        response.setInboundId(inboundReceipt.getInboundId());
        response.setItemCode(inboundReceipt.getItem().getItemCode());
        response.setItemName(inboundReceipt.getItem().getItemName());
        response.setPartnerCode(inboundReceipt.getPartner().getPartnerCode());
        response.setPartnerName(inboundReceipt.getPartner().getPartnerName());
        response.setLocationCode(inboundReceipt.getLocation().getLocationCode());
        response.setInboundQty(inboundReceipt.getInboundQty());
        response.setInboundDate(inboundReceipt.getInboundDate());
        if (inboundReceipt.getStatus() != null) {
            response.setStatus(inboundReceipt.getStatus().name());
        }
        if (inboundReceipt.getWorker() != null) {
            response.setWorkerName(inboundReceipt.getWorker().getUserName());
        }
        response.setCreatedAt(inboundReceipt.getCreatedAt());
        return response;
    }
}

package com.ssafy.demo_app.api.partner.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PartnerUsageResponse {

    private Integer partnerId;
    private long inboundCount;
    private long shippingCount;
    private LocalDateTime lastInboundAt;
    private LocalDateTime lastShippingAt;
    private LocalDateTime lastUsedAt;
    private boolean canDelete;
    private String deleteBlockedReason;
}

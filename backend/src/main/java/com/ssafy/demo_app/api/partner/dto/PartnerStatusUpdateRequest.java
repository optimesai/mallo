package com.ssafy.demo_app.api.partner.dto;

import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerStatusUpdateRequest {

    @NotNull(message = "거래처 상태는 필수입니다.")
    private PartnerMaster.PartnerStatus partnerStatus;
}

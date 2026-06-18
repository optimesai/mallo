package com.ssafy.demo_app.api.partner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerDuplicateCheckResponse {

    private boolean duplicated;

    public PartnerDuplicateCheckResponse(boolean duplicated) {
        this.duplicated = duplicated;
    }
}

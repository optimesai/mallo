package com.ssafy.demo_app.domain.partner.service;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;

import java.util.List;

public interface PartnerService {
    List<PartnerResponse> getPartners(PartnerMaster.PartnerType partnerType, String keyword);
    List<PartnerResponse> searchPartners(String searchValue);
    PartnerResponse createPartner(PartnerRequest request);
    PartnerResponse updatePartner(Integer partnerId, PartnerRequest request);
    void deletePartner(Integer partnerId);
}

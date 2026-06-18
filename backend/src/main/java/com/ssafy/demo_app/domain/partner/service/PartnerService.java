package com.ssafy.demo_app.domain.partner.service;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerShippedItemResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerStatsResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerSuppliedItemResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerUsageResponse;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.global.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartnerService {
    PageResponse<PartnerResponse> getPartners(Pageable pageable, PartnerMaster.PartnerType partnerType,
                                              PartnerMaster.PartnerStatus partnerStatus, Boolean hasBusinessNo,
                                              String keyword);
    PartnerStatsResponse getPartnerStats();
    List<PartnerResponse> searchPartners(String searchValue);
    boolean existsByPartnerCode(String partnerCode);
    PartnerResponse createPartner(PartnerRequest request);
    PartnerResponse updatePartner(Integer partnerId, PartnerRequest request);
    PartnerResponse updatePartnerStatus(Integer partnerId, PartnerMaster.PartnerStatus partnerStatus);
    PartnerUsageResponse getPartnerUsage(Integer partnerId);
    List<PartnerSuppliedItemResponse> getSuppliedItems(Integer partnerId);
    List<PartnerShippedItemResponse> getShippedItems(Integer partnerId);
    void deletePartner(Integer partnerId);
}

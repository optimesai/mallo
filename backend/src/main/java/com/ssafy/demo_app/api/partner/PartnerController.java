package com.ssafy.demo_app.api.partner;

import com.ssafy.demo_app.api.partner.dto.PartnerDuplicateCheckResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerStatusUpdateRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerSuppliedItemResponse;
import com.ssafy.demo_app.api.partner.dto.PartnerUsageResponse;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.service.PartnerService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PartnerController implements PartnerApi {

    private final PartnerService partnerService;

    @Override
    public ResponseEntity<ApiResponse<PageResponse<PartnerResponse>>> getPartners(Pageable pageable,
                                                                                  PartnerMaster.PartnerType partnerType,
                                                                                  PartnerMaster.PartnerStatus partnerStatus,
                                                                                  Boolean hasBusinessNo,
                                                                                  String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                partnerService.getPartners(pageable, partnerType, partnerStatus, hasBusinessNo, keyword)
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<PartnerDuplicateCheckResponse>> checkDuplicate(String partnerCode) {
        return ResponseEntity.ok(ApiResponse.success(
                new PartnerDuplicateCheckResponse(partnerService.existsByPartnerCode(partnerCode))
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<List<PartnerResponse>>> searchPartners(String searchValue) {
        return ResponseEntity.ok(ApiResponse.success(partnerService.searchPartners(searchValue)));
    }

    @Override
    public ResponseEntity<ApiResponse<PartnerResponse>> createPartner(PartnerRequest request) {
        PartnerResponse response = partnerService.createPartner(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("거래처가 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<PartnerResponse>> updatePartner(Integer id, PartnerRequest request) {
        PartnerResponse response = partnerService.updatePartner(id, request);
        return ResponseEntity.ok(ApiResponse.success("거래처가 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<PartnerResponse>> updatePartnerStatus(Integer id, PartnerStatusUpdateRequest request) {
        PartnerResponse response = partnerService.updatePartnerStatus(id, request.getPartnerStatus());
        return ResponseEntity.ok(ApiResponse.success("거래처 상태가 변경되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<PartnerUsageResponse>> getPartnerUsage(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(partnerService.getPartnerUsage(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<PartnerSuppliedItemResponse>>> getSuppliedItems(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(partnerService.getSuppliedItems(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deletePartner(Integer id) {
        partnerService.deletePartner(id);
        return ResponseEntity.ok(ApiResponse.success("거래처가 삭제되었습니다."));
    }
}

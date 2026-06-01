package com.ssafy.demo_app.api.partner;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.service.PartnerService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PartnerController implements PartnerApi {

    private final PartnerService partnerService;

    @Override
    public ResponseEntity<ApiResponse<List<PartnerResponse>>> getPartners(PartnerMaster.PartnerType partnerType, String keyword) {
        return ResponseEntity.ok(ApiResponse.success(partnerService.getPartners(partnerType, keyword)));
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
    public ResponseEntity<ApiResponse<Void>> deletePartner(Integer id) {
        partnerService.deletePartner(id);
        return ResponseEntity.ok(ApiResponse.success("거래처가 삭제되었습니다."));
    }
}

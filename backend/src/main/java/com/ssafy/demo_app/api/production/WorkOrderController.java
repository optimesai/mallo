package com.ssafy.demo_app.api.production;

import com.ssafy.demo_app.domain.production.service.WorkOrderService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WorkOrderController implements WorkOrderApi {

    private final WorkOrderService workOrderService;

    @Override
    public ResponseEntity<ApiResponse<Void>> issueMaterials(
            CustomUserDetails userDetails,
            Integer orderId
    ) {
        workOrderService.issueMaterials(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("BOM 기반 생산 자재 출고 처리가 완료되었습니다."));
    }
}

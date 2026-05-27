package com.ssafy.demo_app.api.production;

import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Work Order API", description = "생산 작업 지시 및 자재 출고 관리 API")
@RequestMapping("/api/work-orders")
public interface WorkOrderApi {

    @Operation(summary = "BOM 기반 자재 출고 처리", description = "생산 작업 지시에 등록된 품목의 BOM 구조를 기준으로 자재 창고 재고를 차감합니다.")
    @PostMapping("/{orderId}/issue-materials")
    ResponseEntity<ApiResponse<Void>> issueMaterials(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "작업 지시 ID", required = true) @PathVariable Integer orderId
    );
}

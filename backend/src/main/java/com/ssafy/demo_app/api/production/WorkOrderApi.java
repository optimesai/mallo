package com.ssafy.demo_app.api.production;

import com.ssafy.demo_app.api.production.dto.WorkOrderCloseRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderCreateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderDetailResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderStatusUpdateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderUpdateRequest;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
@Tag(name = "Work Order API", description = "생산 작업 지시 관리 API")
@RequestMapping("/api/work-orders")
public interface WorkOrderApi {

    @Operation(summary = "작업 지시 생성", description = "생산 품목, 계획 라우팅, 목표 수량, 계획일을 기반으로 작업 지시를 생성합니다. 작업 지시 번호는 서버에서 자동 생성됩니다.")
    @PostMapping
    ResponseEntity<ApiResponse<WorkOrderResponse>> createWorkOrder(
            @Valid @RequestBody WorkOrderCreateRequest request
    );

    @Operation(summary = "작업 지시 목록 조회", description = "상태, 계획일, 기간, 키워드, 공장/라인 기준으로 작업 지시 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<WorkOrderResponse>>> getWorkOrders(
            @PageableDefault(size = 10, sort = "planDate") Pageable pageable,
            @RequestParam(required = false) WorkOrder.OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String itemKeyword,
            @RequestParam(required = false) String factoryName,
            @RequestParam(required = false) String lineName,
            @RequestParam(required = false) String operationName
    );

    @Operation(summary = "작업 지시 상세 조회", description = "작업 지시 기본 정보, BOM 기준 투입량, 생산 실적 이력을 조회합니다.")
    @GetMapping("/{orderKey}")
    ResponseEntity<ApiResponse<WorkOrderDetailResponse>> getWorkOrder(
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey
    );

    @Operation(summary = "작업 지시 수정", description = "READY 상태의 작업 지시만 수정합니다. 작업 지시 번호는 수정할 수 없습니다.")
    @PutMapping("/{orderKey}")
    ResponseEntity<ApiResponse<WorkOrderResponse>> updateWorkOrder(
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey,
            @Valid @RequestBody WorkOrderUpdateRequest request
    );

    @Operation(summary = "작업 지시 삭제", description = "READY 상태이고 실적/불출 이력이 없는 작업 지시만 삭제합니다.")
    @DeleteMapping("/{orderKey}")
    ResponseEntity<ApiResponse<Void>> deleteWorkOrder(
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey
    );

    @Operation(summary = "작업 지시 상태 변경", description = "RUN-HOLD 간 상태 전이를 처리합니다. CLOSE는 마감 API를 사용합니다.")
    @PatchMapping("/{orderKey}/status")
    ResponseEntity<ApiResponse<WorkOrderResponse>> updateStatus(
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey,
            @Valid @RequestBody WorkOrderStatusUpdateRequest request
    );

    @Operation(summary = "작업 지시 마감", description = "RUN/HOLD 작업 지시를 CLOSE로 마감합니다. 목표 미달이면 allowUnderTargetClose=true가 필요합니다.")
    @PutMapping("/{orderKey}/close")
    ResponseEntity<ApiResponse<WorkOrderResponse>> closeWorkOrder(
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey,
            @RequestBody(required = false) WorkOrderCloseRequest request
    );

    @Operation(summary = "BOM 기반 자재 출고 처리", description = "생산 작업 지시에 등록된 품목의 BOM 구조를 기준으로 자재 창고 재고를 차감합니다. 불출량은 생산 투입량으로 간주합니다.")
    @PostMapping("/{orderKey}/issue-materials")
    ResponseEntity<ApiResponse<Void>> issueMaterials(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey
    );

    @Operation(summary = "BOM 기반 자재 출고 취소", description = "마감 전 작업 지시의 생산 자재 출고 이력을 되돌리고 현재고를 복원합니다.")
    @PostMapping("/{orderKey}/issue-materials/cancel")
    ResponseEntity<ApiResponse<Void>> cancelIssueMaterials(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @PathVariable("orderKey") String orderKey
    );
}

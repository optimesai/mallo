package com.ssafy.demo_app.api.shipping;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Outbound Shipping API", description = "완제품 출하 관리 API")
@RequestMapping("/api/shippings")
public interface OutboundShippingApi {

    @Operation(summary = "출하 지시 등록", description = "고객사의 주문 사양에 맞춰 새로운 완제품 출하 지시 정보를 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ShippingResponse>> registerShipping(
            @Valid @RequestBody ShippingCreateRequest request
    );

    @Operation(summary = "출하 지시 목록 조회", description = "전체 출하 지시 내역 목록을 페이징 및 필터링하여 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<ShippingResponse>>> getShippings(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @Parameter(description = "출하 상태 (READY, PICKING, PACKING, INSPECTING, SHIPPED, PARTIALLY_SHIPPED, CANCELED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "출하번호, 품목명/코드 또는 거래처명 검색 키워드")
            @RequestParam(required = false) String keyword
    );

    @Operation(summary = "출하 지시 단건 상세 조회", description = "출하 ID로 특정 출하 지시 내역을 상세 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ShippingResponse>> getShipping(
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "출하 완료 처리 및 재고 차감", description = "피킹이 완료된 완제품 출하 지시 건을 최종 출하 완료 처리하고 전산 재고를 차감합니다.")
    @PutMapping("/{id}/complete")
    ResponseEntity<ApiResponse<Void>> completeShipping(
            @AuthenticationPrincipal com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails userDetails,
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "차량 배정 및 피킹 지시", description = "출하 지시에 배송 차량 번호를 지정하고 최적의 피킹 로케이션을 자동 배정합니다.")
    @PutMapping("/{id}/assign-picking")
    ResponseEntity<ApiResponse<ShippingResponse>> assignPicking(
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest request
    );

    @Operation(summary = "출하 취소", description = "READY 상태의 출하 지시를 취소합니다.")
    @PostMapping("/{id}/cancel")
    ResponseEntity<ApiResponse<Void>> cancelShipping(
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody com.ssafy.demo_app.api.shipping.dto.CancelShippingRequest request
    );

    @Operation(summary = "출하 지시 수정", description = "READY 상태의 출하 지시 정보를 수정합니다.")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<ShippingResponse>> updateShipping(
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody com.ssafy.demo_app.api.shipping.dto.ShippingUpdateRequest request
    );

    @Operation(summary = "부분 출하 처리", description = "PICKING 이상 상태의 출하를 부분 완료 처리합니다.")
    @PostMapping("/{id}/partial-ship")
    ResponseEntity<ApiResponse<Void>> partialShip(
            @AuthenticationPrincipal com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails userDetails,
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody com.ssafy.demo_app.api.shipping.dto.PartialShipRequest request
    );
}

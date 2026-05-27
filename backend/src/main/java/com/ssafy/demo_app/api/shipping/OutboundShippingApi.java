package com.ssafy.demo_app.api.shipping;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Outbound Shipping API", description = "완제품 출하 관리 API")
@RequestMapping("/api/shippings")
public interface OutboundShippingApi {

    @Operation(summary = "출하 지시 등록", description = "고객사의 주문 사양에 맞춰 새로운 완제품 출하 지시 정보를 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ShippingResponse>> registerShipping(
            @Valid @RequestBody ShippingCreateRequest request
    );

    @Operation(summary = "출하 지시 목록 조회", description = "전체 출하 지시 내역 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<List<ShippingResponse>>> getShippings();

    @Operation(summary = "출하 지시 단건 상세 조회", description = "출하 ID로 특정 출하 지시 내역을 상세 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ShippingResponse>> getShipping(
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "출하 완료 처리 및 재고 차감", description = "피킹이 완료된 완제품 출하 지시 건을 최종 출하 완료 처리하고 전산 재고를 차감합니다.")
    @PutMapping("/{id}/complete")
    ResponseEntity<ApiResponse<Void>> completeShipping(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails userDetails,
            @Parameter(description = "출하 ID", required = true) @PathVariable Integer id
    );
}

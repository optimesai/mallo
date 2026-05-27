package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Inbound/Inventory Management API", description = "원부자재 입고 및 재고 적재 관리 API")
@RequestMapping("/api/inbounds")
public interface InventoryApi {

    @Operation(summary = "입고 목록 조회", description = "등록된 전체 입고 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<List<InboundReceiptResponse>>> getInbounds();

    @Operation(summary = "입고 단건 조회", description = "ID로 특정 입고 상세 내역을 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<InboundReceiptResponse>> getInbound(
            @Parameter(description = "입고 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "입고 예정 등록", description = "외주 공급사로부터 도착한 원자재 명세를 기반으로 입고 예정 등록을 수행합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<InboundReceiptResponse>> registerInbound(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InboundCreateRequest request
    );

    @Operation(summary = "입고 완료 처리", description = "입고 예정 상태(READY)인 데이터를 검수 완료하여 입고 완료(COMPLETED) 처리합니다.")
    @PutMapping("/{id}/complete")
    ResponseEntity<ApiResponse<InboundReceiptResponse>> completeInbound(
            @Parameter(description = "입고 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "재고 적재 및 로케이션 지정", description = "입고가 완료된 자재를 가용 창고의 특정 로케이션 렉(Rack) 위치에 바인딩하여 실시간 재고를 증가시킵니다.")
    @PostMapping("/{id}/stack")
    ResponseEntity<ApiResponse<Void>> stackInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "입고 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody InventoryStackRequest request
    );

    @Operation(summary = "입고 정보 삭제", description = "READY 상태의 입고 예정 정보만 삭제 가능합니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteInbound(
            @Parameter(description = "입고 ID", required = true) @PathVariable Integer id
    );
}

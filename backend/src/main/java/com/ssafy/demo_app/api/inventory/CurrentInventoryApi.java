package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Current Inventory API", description = "실시간 현재 재고 및 수불 이력 조회 API")
@RequestMapping("/api/inventory")
public interface CurrentInventoryApi {

    @Operation(summary = "현재 재고 목록 조회", description = "창고 전체의 현재 실시간 재고 목록을 페이징 및 키워드 검색으로 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<CurrentInventoryResponse>>> getInventories(
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable,
            @Parameter(description = "품목명 또는 품목코드 검색 키워드")
            @RequestParam(required = false) String keyword
    );

    @Operation(summary = "현재 재고 단건 조회", description = "특정 품목-로케이션 바인딩 재고 건을 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<CurrentInventoryResponse>> getInventory(
            @Parameter(description = "재고 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "수불 이력 목록 조회", description = "입출고, 적재 등으로 발생한 수불 트랜잭션 히스토리를 페이징 및 필터링하여 조회합니다.")
    @GetMapping("/history")
    ResponseEntity<ApiResponse<PageResponse<TransactionHistoryResponse>>> getTransactionHistories(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @Parameter(description = "거래 유형 (INBOUND, OUTBOUND, PRODUCTION_ISSUE)")
            @RequestParam(required = false) String transactionType,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );
}

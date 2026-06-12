package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.domain.inventory.service.InboundService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class InboundController implements InboundApi {

    private final InboundService inboundService;

    @Override
    public ResponseEntity<ApiResponse<PageResponse<InboundReceiptResponse>>> getInbounds(
            Pageable pageable, String status, String keyword,
            LocalDate startDate, LocalDate endDate) {
        PageResponse<InboundReceiptResponse> page = inboundService.getInbounds(pageable, status, keyword, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Override
    public ResponseEntity<ApiResponse<InboundReceiptResponse>> getInbound(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inboundService.getInbound(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<InboundReceiptResponse>> registerInbound(
            @AuthenticationPrincipal CustomUserDetails userDetails, InboundCreateRequest request) {
        InboundReceiptResponse response = inboundService.registerInbound(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<InboundReceiptResponse>> completeInbound(Integer id) {
        InboundReceiptResponse response = inboundService.completeInbound(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> stackInventory(
            CustomUserDetails userDetails, Integer id, InventoryStackRequest request) {
        inboundService.stackInventory(userDetails.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("재고 적재가 완료되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteInbound(Integer id) {
        inboundService.deleteInbound(id);
        return ResponseEntity.ok(ApiResponse.success("입고 정보가 삭제되었습니다."));
    }
}

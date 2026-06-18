package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.InventoryAdjustRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryScrapRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryTransferRequest;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InventoryManagementController implements InventoryManagementApi {

    private final InventoryService inventoryService;

    @Override
    public ResponseEntity<ApiResponse<Void>> adjustInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            InventoryAdjustRequest request) {
        inventoryService.adjustInventory(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("재고가 조정되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> transferInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            InventoryTransferRequest request) {
        inventoryService.transferInventory(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("재고가 이동되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> scrapInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            InventoryScrapRequest request) {
        inventoryService.scrapInventory(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("재고가 폐기 처리되었습니다."));
    }
}

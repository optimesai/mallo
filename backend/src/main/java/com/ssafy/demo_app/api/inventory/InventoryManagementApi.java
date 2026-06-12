package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.InventoryAdjustRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryScrapRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryTransferRequest;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Inventory Management API", description = "재고 조정, 이동, 폐기 관리 API")
@RequestMapping("/api/inventories")
public interface InventoryManagementApi {

    @Operation(summary = "재고 조정", description = "실사 차이 등으로 재고 수량을 조정합니다.")
    @PostMapping("/adjust")
    ResponseEntity<ApiResponse<Void>> adjustInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InventoryAdjustRequest request
    );

    @Operation(summary = "로케이션 간 재고 이동", description = "지정된 로케이션 간 재고를 이동합니다.")
    @PostMapping("/transfer")
    ResponseEntity<ApiResponse<Void>> transferInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InventoryTransferRequest request
    );

    @Operation(summary = "재고 폐기 처리", description = "불량품 등을 폐기 처리합니다.")
    @PostMapping("/scrap")
    ResponseEntity<ApiResponse<Void>> scrapInventory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InventoryScrapRequest request
    );
}

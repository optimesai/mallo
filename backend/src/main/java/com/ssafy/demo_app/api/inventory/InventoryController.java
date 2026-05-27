package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inbounds")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InboundReceiptResponse>>> getInbounds() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInbounds()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InboundReceiptResponse>> getInbound(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInbound(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InboundReceiptResponse>> registerInbound(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InboundCreateRequest request
    ) {
        InboundReceiptResponse response = inventoryService.registerInbound(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("입고 예정 정보가 등록되었습니다.", response));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<InboundReceiptResponse>> completeInbound(@PathVariable Integer id) {
        InboundReceiptResponse response = inventoryService.completeInbound(id);
        return ResponseEntity.ok(ApiResponse.success("입고 처리가 완료되었습니다.", response));
    }
}

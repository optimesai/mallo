package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class CurrentInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CurrentInventoryResponse>>> getInventories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrentInventoryResponse>> getInventory(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(id)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TransactionHistoryResponse>>> getTransactionHistories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getTransactionHistories()));
    }
}

package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CurrentInventoryController implements CurrentInventoryApi {

    private final InventoryService inventoryService;

    @Override
    public ResponseEntity<ApiResponse<List<CurrentInventoryResponse>>> getInventories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventories()));
    }

    @Override
    public ResponseEntity<ApiResponse<CurrentInventoryResponse>> getInventory(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<TransactionHistoryResponse>>> getTransactionHistories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getTransactionHistories()));
    }
}

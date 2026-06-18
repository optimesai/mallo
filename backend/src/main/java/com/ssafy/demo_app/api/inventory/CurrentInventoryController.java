package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class CurrentInventoryController implements CurrentInventoryApi {

    private final InventoryService inventoryService;

    @Override
    public ResponseEntity<ApiResponse<PageResponse<CurrentInventoryResponse>>> getInventories(
            Pageable pageable,
            String keyword
    ) {
        PageResponse<CurrentInventoryResponse> page = inventoryService.getInventories(pageable, keyword);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Override
    public ResponseEntity<ApiResponse<CurrentInventoryResponse>> getInventory(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<PageResponse<TransactionHistoryResponse>>> getTransactionHistories(
            Pageable pageable,
            String transactionType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        PageResponse<TransactionHistoryResponse> page = inventoryService.getTransactionHistories(pageable, transactionType, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}

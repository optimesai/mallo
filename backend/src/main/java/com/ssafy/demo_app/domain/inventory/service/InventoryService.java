package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryAdjustRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryScrapRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.api.inventory.dto.InventoryTransferRequest;
import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;
import com.ssafy.demo_app.global.response.PageResponse;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface InventoryService {

    PageResponse<InboundReceiptResponse> getInbounds(Pageable pageable, String status, String keyword,
                                                     LocalDate startDate, LocalDate endDate);

    InboundReceiptResponse getInbound(Integer inboundId);

    InboundReceiptResponse registerInbound(Integer workerId, InboundCreateRequest request);

    InboundReceiptResponse completeInbound(Integer inboundId);

    void stackInventory(Integer workerId, Integer inboundId, InventoryStackRequest request);

    void deleteInbound(Integer inboundId);

    void adjustInventory(Integer workerId, InventoryAdjustRequest request);

    void transferInventory(Integer workerId, InventoryTransferRequest request);

    void scrapInventory(Integer workerId, InventoryScrapRequest request);

    PageResponse<CurrentInventoryResponse> getInventories(Pageable pageable, String keyword);

    CurrentInventoryResponse getInventory(Integer inventoryId);

    PageResponse<TransactionHistoryResponse> getTransactionHistories(Pageable pageable, String transactionType,
                                                                     LocalDate startDate, LocalDate endDate);

    PageResponse<LocationResponse> getLocations(Pageable pageable, String keyword);

    LocationResponse getLocation(Integer locationId);

    LocationResponse createLocation(LocationRequest request);

    LocationResponse updateLocation(Integer locationId, LocationRequest request);

    void deleteLocation(Integer locationId);
}

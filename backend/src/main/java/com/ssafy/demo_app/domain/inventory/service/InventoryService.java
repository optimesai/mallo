package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;

import java.util.List;

public interface InventoryService {

    List<InboundReceiptResponse> getInbounds();

    InboundReceiptResponse getInbound(Integer inboundId);

    InboundReceiptResponse registerInbound(Integer workerId, InboundCreateRequest request);

    InboundReceiptResponse completeInbound(Integer inboundId);

    void stackInventory(Integer workerId, Integer inboundId, InventoryStackRequest request);

    void deleteInbound(Integer inboundId);

    List<CurrentInventoryResponse> getInventories();

    CurrentInventoryResponse getInventory(Integer inventoryId);

    List<TransactionHistoryResponse> getTransactionHistories();

    List<LocationResponse> getLocations();

    LocationResponse getLocation(Integer locationId);

    LocationResponse createLocation(LocationRequest request);

    LocationResponse updateLocation(Integer locationId, LocationRequest request);

    void deleteLocation(Integer locationId);
}

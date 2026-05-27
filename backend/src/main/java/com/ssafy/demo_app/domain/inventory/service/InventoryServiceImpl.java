package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.api.inventory.dto.CurrentInventoryResponse;
import com.ssafy.demo_app.api.inventory.dto.TransactionHistoryResponse;
import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.WarehouseLocationRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final int MAX_LOCATION_CAPACITY = 1000;

    private final InboundReceiptRepository inboundReceiptRepository;
    private final ItemMasterRepository itemMasterRepository;
    private final PartnerMasterRepository partnerMasterRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final UserRepository userRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;

    @Override
    public List<InboundReceiptResponse> getInbounds() {
        return inboundReceiptRepository.findAll().stream()
                .map(InboundReceiptResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public InboundReceiptResponse getInbound(Integer inboundId) {
        InboundReceipt inboundReceipt = inboundReceiptRepository.findById(inboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBOUND_NOT_FOUND));
        return InboundReceiptResponse.from(inboundReceipt);
    }

    @Override
    @Transactional
    public InboundReceiptResponse registerInbound(Integer workerId, InboundCreateRequest request) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        ItemMaster item = itemMasterRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        PartnerMaster partner = partnerMasterRepository.findByPartnerCode(request.getPartnerCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        WarehouseLocation location = warehouseLocationRepository.findByLocationCode(request.getLocationCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));

        InboundReceipt receipt = new InboundReceipt();
        receipt.setItem(item);
        receipt.setPartner(partner);
        receipt.setLocation(location);
        receipt.setInboundQty(request.getInboundQty());
        receipt.setInboundDate(request.getInboundDate());
        receipt.setWorker(worker);
        receipt.setStatus(InboundReceipt.InboundStatus.READY);

        return InboundReceiptResponse.from(inboundReceiptRepository.save(receipt));
    }

    @Override
    @Transactional
    public InboundReceiptResponse completeInbound(Integer inboundId) {
        InboundReceipt receipt = inboundReceiptRepository.findById(inboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBOUND_NOT_FOUND));
        if (receipt.getStatus() != InboundReceipt.InboundStatus.READY) {
            throw new BusinessException(ErrorCode.INBOUND_STATUS_INVALID);
        }
        receipt.setStatus(InboundReceipt.InboundStatus.COMPLETED);
        return InboundReceiptResponse.from(inboundReceiptRepository.save(receipt));
    }

    @Override
    @Transactional
    public void stackInventory(Integer workerId, Integer inboundId, InventoryStackRequest request) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        InboundReceipt receipt = inboundReceiptRepository.findById(inboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBOUND_NOT_FOUND));
        if (receipt.getStatus() != InboundReceipt.InboundStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INBOUND_STATUS_INVALID);
        }
        WarehouseLocation targetLocation = warehouseLocationRepository.findByLocationCode(request.getTargetLocationCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));

        ItemMaster item = receipt.getItem();

        CurrentInventory inventory = currentInventoryRepository.findByItemAndLocation(item, targetLocation)
                .orElseGet(() -> {
                    CurrentInventory ci = new CurrentInventory();
                    ci.setItem(item);
                    ci.setLocation(targetLocation);
                    ci.setCurrentQty(0);
                    return ci;
                });

        if (inventory.getCurrentQty() + receipt.getInboundQty() > MAX_LOCATION_CAPACITY) {
            throw new BusinessException(ErrorCode.LOCATION_CAPACITY_EXCEEDED);
        }

        inventory.setCurrentQty(inventory.getCurrentQty() + receipt.getInboundQty());
        currentInventoryRepository.save(inventory);

        receipt.setLocation(targetLocation);
        inboundReceiptRepository.save(receipt);

        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(item);
        history.setLocation(targetLocation);
        history.setTransactionType(InventoryTransactionHistory.TransactionType.INBOUND);
        history.setQuantity(receipt.getInboundQty());
        history.setReasonDesc("Inbound receipt stacked to rack");
        history.setWorker(worker);
        transactionHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void deleteInbound(Integer inboundId) {
        InboundReceipt receipt = inboundReceiptRepository.findById(inboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBOUND_NOT_FOUND));
        if (receipt.getStatus() != InboundReceipt.InboundStatus.READY) {
            throw new BusinessException(ErrorCode.INBOUND_CANNOT_DELETE);
        }
        inboundReceiptRepository.delete(receipt);
    }

    @Override
    public List<CurrentInventoryResponse> getInventories() {
        return currentInventoryRepository.findAll().stream()
                .map(CurrentInventoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public CurrentInventoryResponse getInventory(Integer inventoryId) {
        CurrentInventory inventory = currentInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        return CurrentInventoryResponse.from(inventory);
    }

    @Override
    public List<TransactionHistoryResponse> getTransactionHistories() {
        return transactionHistoryRepository.findAll().stream()
                .map(TransactionHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationResponse> getLocations() {
        return warehouseLocationRepository.findAll().stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public LocationResponse getLocation(Integer locationId) {
        WarehouseLocation location = warehouseLocationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));
        return LocationResponse.from(location);
    }

    @Override
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        if (warehouseLocationRepository.existsByLocationCode(request.getLocationCode())) {
            throw new BusinessException(ErrorCode.LOCATION_CODE_DUPLICATE);
        }
        WarehouseLocation location = new WarehouseLocation();
        location.setLocationCode(request.getLocationCode());
        location.setWarehouseName(request.getWarehouseName());
        location.setRackRow(request.getRackRow());
        location.setRackColumn(request.getRackColumn());
        return LocationResponse.from(warehouseLocationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(Integer locationId, LocationRequest request) {
        WarehouseLocation location = warehouseLocationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));
        if (warehouseLocationRepository.existsByLocationCodeAndLocationIdNot(request.getLocationCode(), locationId)) {
            throw new BusinessException(ErrorCode.LOCATION_CODE_DUPLICATE);
        }
        location.setLocationCode(request.getLocationCode());
        location.setWarehouseName(request.getWarehouseName());
        location.setRackRow(request.getRackRow());
        location.setRackColumn(request.getRackColumn());
        return LocationResponse.from(warehouseLocationRepository.save(location));
    }

    @Override
    @Transactional
    public void deleteLocation(Integer locationId) {
        WarehouseLocation location = warehouseLocationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));
        if (currentInventoryRepository.existsByLocation(location)) {
            throw new BusinessException(ErrorCode.LOCATION_HAS_INVENTORY);
        }
        warehouseLocationRepository.delete(location);
    }
}

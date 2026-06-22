package com.ssafy.demo_app.domain.shipping.service;
import com.ssafy.demo_app.api.shipping.dto.CancelShippingRequest;
import com.ssafy.demo_app.api.shipping.dto.PartialShipRequest;
import com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.api.shipping.dto.ShippingUpdateRequest;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.shipping.entity.ShippingType;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundShippingServiceImpl implements OutboundShippingService {

    private final OutboundShippingRepository outboundShippingRepository;
    private final PartnerMasterRepository partnerMasterRepository;
    private final ItemMasterRepository itemMasterRepository;
    private final UserRepository userRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;

    @Override
    @Transactional
    public ShippingResponse registerShipping(ShippingCreateRequest request) {
        if (outboundShippingRepository.existsByShippingNo(request.getShippingNo())) {
            throw new BusinessException(ErrorCode.SHIPPING_NO_DUPLICATE);
        }

        PartnerMaster partner = partnerMasterRepository.findByPartnerCode(request.getPartnerCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        validateShippingPartner(partner);

        ItemMaster item = itemMasterRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo(request.getShippingNo());
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(request.getRequestQty());
        if (request.getShippingType() != null) {
            shipping.setShippingType(com.ssafy.demo_app.domain.shipping.entity.ShippingType.valueOf(request.getShippingType()));
        }
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);

        OutboundShipping savedShipping = outboundShippingRepository.save(shipping);
        return ShippingResponse.from(savedShipping);
    }

    @Override
    public PageResponse<ShippingResponse> getShippings(Pageable pageable, String status, String keyword) {
        String normalizedStatus = normalizeShippingStatus(status);
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<ShippingResponse> page = outboundShippingRepository
                .findShippingList(normalizedStatus, normalizedKeyword, sanitizeShippingPageable(pageable))
                .map(ShippingResponse::from);
        return PageResponse.from(page);
    }

    @Override
    public ShippingResponse getShipping(Integer shippingId) {
        return outboundShippingRepository.findById(shippingId)
                .map(ShippingResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));
    }

    @Override
    @Transactional
    public void completeShipping(Integer shippingId, Integer workerId) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (shipping.getStatus() != OutboundShipping.ShippingStatus.PICKING
                && shipping.getStatus() != OutboundShipping.ShippingStatus.PACKING
                && shipping.getStatus() != OutboundShipping.ShippingStatus.INSPECTING) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }

        int completedQty = shipping.getRequestQty();
        shipping.setShippedQty(completedQty);

        // Update shipping
        shipping.setStatus(OutboundShipping.ShippingStatus.SHIPPED);
        shipping.setShippedAt(java.time.LocalDateTime.now());
        shipping.setWorker(worker);
        outboundShippingRepository.save(shipping);

        // Log transaction history (inventory already deducted at PICKING stage)
        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(shipping.getItem());
        history.setLocation(shipping.getPickingLocation());
        history.setTransactionType(TransactionType.OUTBOUND);
        history.setQuantity(completedQty);
        history.setReasonDesc("Outbound shipping complete. ShippingNo: " + shipping.getShippingNo());
        history.setWorker(worker);
        transactionHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public ShippingResponse assignPicking(Integer shippingId, PickingAssignRequest request) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));

        if (shipping.getStatus() != OutboundShipping.ShippingStatus.READY) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }

        // FIFO multi-location picking: get inventory sorted by firstInboundDate ASC
        List<CurrentInventory> inventories = currentInventoryRepository
                .findByItemOrderByFirstInboundDateAsc(shipping.getItem());
        if (inventories.isEmpty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int remainingQty = shipping.getRequestQty();
        WarehouseLocation primaryLocation = null;
        List<String> pickingDetails = new ArrayList<>();

        for (CurrentInventory inv : inventories) {
            if (remainingQty <= 0) break;
            if (inv.getCurrentQty() <= 0) continue;

            int pickQty = Math.min(remainingQty, inv.getCurrentQty());
            inv.setCurrentQty(inv.getCurrentQty() - pickQty);
            currentInventoryRepository.save(inv);

            if (primaryLocation == null) {
                primaryLocation = inv.getLocation();
            }

            // Log RESERVATION transaction history for this pick
            InventoryTransactionHistory history = new InventoryTransactionHistory();
            history.setItem(shipping.getItem());
            history.setLocation(inv.getLocation());
            history.setTransactionType(TransactionType.RESERVATION);
            history.setQuantity(pickQty);
            history.setReasonDesc("FIFO picking reservation. ShippingNo: " + shipping.getShippingNo()
                    + ", Lot: " + (inv.getLotNumber() != null ? inv.getLotNumber() : "N/A"));
            history.setWorker(null); // worker assigned at complete
            transactionHistoryRepository.save(history);

            pickingDetails.add(inv.getLocation().getLocationCode()
                    + ":" + pickQty
                    + ":" + (inv.getLotNumber() != null ? inv.getLotNumber() : "N/A"));

            remainingQty -= pickQty;
        }

        if (remainingQty > 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        shipping.setPickingLocation(primaryLocation);
        shipping.setVehicleNo(request.getVehicleNo());
        shipping.setStatus(OutboundShipping.ShippingStatus.PICKING);
        shipping.setShippedQty(0);

        OutboundShipping savedShipping = outboundShippingRepository.save(shipping);
        return ShippingResponse.from(savedShipping);
    }

    @Override
    @Transactional
    public void cancelShipping(Integer shippingId, CancelShippingRequest request) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));

        if (shipping.getStatus() != OutboundShipping.ShippingStatus.READY) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }

        shipping.setStatus(OutboundShipping.ShippingStatus.CANCELED);
        shipping.setCancelReason(request.getCancelReason());
        outboundShippingRepository.save(shipping);
    }

    @Override
    @Transactional
    public ShippingResponse updateShipping(Integer shippingId, ShippingUpdateRequest request) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));

        if (shipping.getStatus() != OutboundShipping.ShippingStatus.READY) {
            throw new BusinessException(ErrorCode.SHIPPING_CANNOT_MODIFY);
        }

        if (request.getRequestQty() != null) shipping.setRequestQty(request.getRequestQty());
        if (request.getShippingType() != null) shipping.setShippingType(ShippingType.valueOf(request.getShippingType()));
        if (request.getCarrier() != null) shipping.setCarrier(request.getCarrier());
        if (request.getTrackingNo() != null) shipping.setTrackingNo(request.getTrackingNo());
        if (request.getEstimatedDelivery() != null) shipping.setEstimatedDelivery(request.getEstimatedDelivery());

        return ShippingResponse.from(outboundShippingRepository.save(shipping));
    }

    @Override
    @Transactional
    public void partialShip(Integer shippingId, Integer workerId, PartialShipRequest request) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (shipping.getStatus() != OutboundShipping.ShippingStatus.PICKING
                && shipping.getStatus() != OutboundShipping.ShippingStatus.PACKING
                && shipping.getStatus() != OutboundShipping.ShippingStatus.INSPECTING) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }

        int shippedSoFar = shipping.getShippedQty() != null ? shipping.getShippedQty() : 0;
        int newShipped = shippedSoFar + request.getShipQty();
        if (newShipped > shipping.getRequestQty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        shipping.setShippedQty(newShipped);
        if (request.getCarrier() != null) shipping.setCarrier(request.getCarrier());
        if (request.getTrackingNo() != null) shipping.setTrackingNo(request.getTrackingNo());
        if (request.getEstimatedDelivery() != null) shipping.setEstimatedDelivery(request.getEstimatedDelivery());
        shipping.setWorker(worker);

        if (newShipped >= shipping.getRequestQty()) {
            shipping.setStatus(OutboundShipping.ShippingStatus.SHIPPED);
        } else {
            shipping.setStatus(OutboundShipping.ShippingStatus.PARTIALLY_SHIPPED);
        }
        shipping.setShippedAt(java.time.LocalDateTime.now());
        outboundShippingRepository.save(shipping);

        // Log partial shipment as OUTBOUND transaction
        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(shipping.getItem());
        history.setLocation(shipping.getPickingLocation());
        history.setTransactionType(TransactionType.OUTBOUND);
        history.setQuantity(request.getShipQty());
        history.setReasonDesc("Partial shipment. ShippingNo: " + shipping.getShippingNo());
        history.setWorker(worker);
        transactionHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void packShipping(Integer shippingId, Integer workerId) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));
        if (shipping.getStatus() != OutboundShipping.ShippingStatus.PICKING) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        shipping.setStatus(OutboundShipping.ShippingStatus.PACKING);
        shipping.setWorker(worker);
        outboundShippingRepository.save(shipping);
    }

    @Override
    @Transactional
    public void inspectShipping(Integer shippingId, Integer workerId) {
        OutboundShipping shipping = outboundShippingRepository.findById(shippingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_FOUND));
        if (shipping.getStatus() != OutboundShipping.ShippingStatus.PACKING) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        shipping.setStatus(OutboundShipping.ShippingStatus.INSPECTING);
        shipping.setWorker(worker);
        outboundShippingRepository.save(shipping);
    }

    private String normalizeShippingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalizedStatus = status.trim();
        try {
            OutboundShipping.ShippingStatus.valueOf(normalizedStatus);
            return normalizedStatus;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private Pageable sanitizeShippingPageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private void validateShippingPartner(PartnerMaster partner) {
        if (partner.getPartnerType() != PartnerMaster.PartnerType.CUSTOMER) {
            throw new BusinessException(ErrorCode.PARTNER_TYPE_INVALID);
        }
        if (partner.getPartnerStatus() != PartnerMaster.PartnerStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PARTNER_STATUS_INACTIVE);
        }
    }
}

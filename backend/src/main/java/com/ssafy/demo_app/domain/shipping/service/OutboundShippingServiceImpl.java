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

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        ItemMaster item = itemMasterRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo(request.getShippingNo());
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(request.getRequestQty());
        if (request.getShippingType() != null) {
            shipping.setShippingType(OutboundShipping.ShippingType.valueOf(request.getShippingType()));
        }
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);

        OutboundShipping savedShipping = outboundShippingRepository.save(shipping);
        return ShippingResponse.from(savedShipping);
    }

    @Override
    public PageResponse<ShippingResponse> getShippings(Pageable pageable, String status, String keyword) {
        Specification<OutboundShipping> spec = buildShippingSpec(status, keyword);
        Page<OutboundShipping> page = outboundShippingRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(ShippingResponse::from));
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

        if (shipping.getShippedQty() == null) {
            shipping.setShippedQty(shipping.getRequestQty());
        }

        // Update shipping
        shipping.setStatus(shipping.getShippedQty() < shipping.getRequestQty()
                ? OutboundShipping.ShippingStatus.PARTIALLY_SHIPPED
                : OutboundShipping.ShippingStatus.SHIPPED);
        shipping.setShippedAt(java.time.LocalDateTime.now());
        shipping.setWorker(worker);
        outboundShippingRepository.save(shipping);

        // Log transaction history (inventory already deducted at PICKING stage)
        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(shipping.getItem());
        history.setLocation(shipping.getPickingLocation());
        history.setTransactionType(TransactionType.OUTBOUND);
        history.setQuantity(shipping.getShippedQty());
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

    // --- Specification builder ---

    private Specification<OutboundShipping> buildShippingSpec(String status, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"),
                        OutboundShipping.ShippingStatus.valueOf(status)));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Join<OutboundShipping, ItemMaster> itemJoin = root.join("item");
                Join<OutboundShipping, PartnerMaster> partnerJoin = root.join("partner");
                Predicate shippingNoMatch = cb.like(cb.lower(root.get("shippingNo")), pattern);
                Predicate itemCodeMatch = cb.like(cb.lower(itemJoin.get("itemCode")), pattern);
                Predicate itemNameMatch = cb.like(cb.lower(itemJoin.get("itemName")), pattern);
                Predicate partnerMatch = cb.like(cb.lower(partnerJoin.get("partnerName")), pattern);
                predicates.add(cb.or(shippingNoMatch, itemCodeMatch, itemNameMatch, partnerMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

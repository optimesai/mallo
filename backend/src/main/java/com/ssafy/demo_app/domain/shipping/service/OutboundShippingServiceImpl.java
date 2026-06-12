package com.ssafy.demo_app.domain.shipping.service;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;

import com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
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

        if (shipping.getStatus() != OutboundShipping.ShippingStatus.PICKING) {
            throw new BusinessException(ErrorCode.SHIPPING_STATUS_INVALID);
        }

        if (shipping.getPickingLocation() == null) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_FOUND);
        }

        CurrentInventory inventory = currentInventoryRepository
                .findByItemAndLocation(shipping.getItem(), shipping.getPickingLocation())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK));

        if (inventory.getCurrentQty() < shipping.getRequestQty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        // Deduct inventory
        inventory.setCurrentQty(inventory.getCurrentQty() - shipping.getRequestQty());
        currentInventoryRepository.save(inventory);

        // Update shipping
        shipping.setStatus(OutboundShipping.ShippingStatus.SHIPPED);
        shipping.setShippedAt(java.time.LocalDateTime.now());
        shipping.setWorker(worker);
        outboundShippingRepository.save(shipping);

        // Log transaction history
        InventoryTransactionHistory history = new InventoryTransactionHistory();
        history.setItem(shipping.getItem());
        history.setLocation(shipping.getPickingLocation());
        history.setTransactionType(TransactionType.OUTBOUND);
        history.setQuantity(shipping.getRequestQty());
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

        // Find location with enough inventory for target item
        List<CurrentInventory> inventories = currentInventoryRepository.findByItem(shipping.getItem());
        CurrentInventory targetInventory = inventories.stream()
                .filter(inv -> inv.getCurrentQty() >= shipping.getRequestQty())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK));

        shipping.setPickingLocation(targetInventory.getLocation());
        shipping.setVehicleNo(request.getVehicleNo());
        shipping.setStatus(OutboundShipping.ShippingStatus.PICKING);

        OutboundShipping savedShipping = outboundShippingRepository.save(shipping);
        return ShippingResponse.from(savedShipping);
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

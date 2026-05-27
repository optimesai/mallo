package com.ssafy.demo_app.domain.shipping.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);

        OutboundShipping savedShipping = outboundShippingRepository.save(shipping);
        return ShippingResponse.from(savedShipping);
    }

    @Override
    public List<ShippingResponse> getShippings() {
        return outboundShippingRepository.findAll().stream()
                .map(ShippingResponse::from)
                .toList();
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

        if (shipping.getStatus() == OutboundShipping.ShippingStatus.SHIPPED) {
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
        history.setTransactionType(InventoryTransactionHistory.TransactionType.OUTBOUND);
        history.setQuantity(shipping.getRequestQty());
        history.setReasonDesc("Outbound shipping complete. ShippingNo: " + shipping.getShippingNo());
        history.setWorker(worker);
        transactionHistoryRepository.save(history);
    }
}

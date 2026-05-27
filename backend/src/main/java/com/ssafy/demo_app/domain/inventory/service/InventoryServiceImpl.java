package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
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

    private final InboundReceiptRepository inboundReceiptRepository;
    private final ItemMasterRepository itemMasterRepository;
    private final PartnerMasterRepository partnerMasterRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final UserRepository userRepository;

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
}

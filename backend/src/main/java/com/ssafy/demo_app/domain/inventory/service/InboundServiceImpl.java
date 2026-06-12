package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
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
import com.ssafy.demo_app.global.response.PageResponse;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InboundServiceImpl implements InboundService {

    private static final int MAX_LOCATION_CAPACITY = 99999;

    private final InboundReceiptRepository inboundReceiptRepository;
    private final ItemMasterRepository itemMasterRepository;
    private final PartnerMasterRepository partnerMasterRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final UserRepository userRepository;
    private final CurrentInventoryRepository currentInventoryRepository;
    private final InventoryTransactionHistoryRepository transactionHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundReceiptResponse> getInbounds(Pageable pageable, String status, String keyword,
                                                            LocalDate startDate, LocalDate endDate) {
        Specification<InboundReceipt> spec = buildInboundSpec(status, keyword, startDate, endDate);
        Page<InboundReceipt> page = inboundReceiptRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(InboundReceiptResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
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
        receipt.setStatus(InboundReceipt.InboundStatus.STACKED);
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

    private Specification<InboundReceipt> buildInboundSpec(String status, String keyword,
                                                           LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), InboundReceipt.InboundStatus.valueOf(status)));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate itemPredicate = cb.like(cb.lower(root.get("item").get("itemName")), pattern);
                Predicate itemCodePredicate = cb.like(cb.lower(root.get("item").get("itemCode")), pattern);
                Predicate partnerPredicate = cb.like(cb.lower(root.get("partner").get("partnerName")), pattern);
                predicates.add(cb.or(itemPredicate, itemCodePredicate, partnerPredicate));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("inboundDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("inboundDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

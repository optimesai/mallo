package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
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
}

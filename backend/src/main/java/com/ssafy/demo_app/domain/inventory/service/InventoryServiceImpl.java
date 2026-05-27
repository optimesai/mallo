package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
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
}

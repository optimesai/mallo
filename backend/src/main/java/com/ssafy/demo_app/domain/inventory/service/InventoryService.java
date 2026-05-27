package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;

import java.util.List;

public interface InventoryService {

    List<InboundReceiptResponse> getInbounds();

    InboundReceiptResponse getInbound(Integer inboundId);
}

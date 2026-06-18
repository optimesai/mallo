package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.global.response.PageResponse;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface InboundService {

    PageResponse<InboundReceiptResponse> getInbounds(Pageable pageable, String status, String keyword,
                                                     LocalDate startDate, LocalDate endDate);

    InboundReceiptResponse getInbound(Integer inboundId);

    InboundReceiptResponse registerInbound(Integer workerId, InboundCreateRequest request);

    InboundReceiptResponse completeInbound(Integer inboundId);

    void stackInventory(Integer workerId, Integer inboundId, InventoryStackRequest request);

    void deleteInbound(Integer inboundId);
}

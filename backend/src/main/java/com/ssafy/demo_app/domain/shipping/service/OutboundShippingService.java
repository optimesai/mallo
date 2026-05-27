package com.ssafy.demo_app.domain.shipping.service;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;

import java.util.List;

public interface OutboundShippingService {
    ShippingResponse registerShipping(ShippingCreateRequest request);
    List<ShippingResponse> getShippings();
    ShippingResponse getShipping(Integer shippingId);
    void completeShipping(Integer shippingId, Integer workerId);
}


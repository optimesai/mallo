package com.ssafy.demo_app.domain.shipping.service;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;

public interface OutboundShippingService {
    ShippingResponse registerShipping(ShippingCreateRequest request);
}

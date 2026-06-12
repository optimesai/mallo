package com.ssafy.demo_app.domain.shipping.service;

import com.ssafy.demo_app.api.shipping.dto.*;

import com.ssafy.demo_app.global.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OutboundShippingService {
    ShippingResponse registerShipping(ShippingCreateRequest request);
    PageResponse<ShippingResponse> getShippings(Pageable pageable, String status, String keyword);
    ShippingResponse getShipping(Integer shippingId);
    void completeShipping(Integer shippingId, Integer workerId);
    ShippingResponse assignPicking(Integer shippingId, PickingAssignRequest request);
    void cancelShipping(Integer shippingId, CancelShippingRequest request);
    ShippingResponse updateShipping(Integer shippingId, ShippingUpdateRequest request);
    void partialShip(Integer shippingId, Integer workerId, PartialShipRequest request);
}

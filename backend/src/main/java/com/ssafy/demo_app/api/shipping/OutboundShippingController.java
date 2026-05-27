package com.ssafy.demo_app.api.shipping;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.domain.shipping.service.OutboundShippingService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OutboundShippingController implements OutboundShippingApi {

    private final OutboundShippingService outboundShippingService;

    @Override
    public ResponseEntity<ApiResponse<ShippingResponse>> registerShipping(
            ShippingCreateRequest request
    ) {
        ShippingResponse response = outboundShippingService.registerShipping(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("출하 지시가 성공적으로 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ShippingResponse>>> getShippings() {
        return ResponseEntity.ok(ApiResponse.success(outboundShippingService.getShippings()));
    }

    @Override
    public ResponseEntity<ApiResponse<ShippingResponse>> getShipping(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(outboundShippingService.getShipping(id)));
    }
}

package com.ssafy.demo_app.api.shipping;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Outbound Shipping API", description = "완제품 출하 관리 API")
@RequestMapping("/api/shippings")
public interface OutboundShippingApi {

    @Operation(summary = "출하 지시 등록", description = "고객사의 주문 사양에 맞춰 새로운 완제품 출하 지시 정보를 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ShippingResponse>> registerShipping(
            @Valid @RequestBody ShippingCreateRequest request
    );
}

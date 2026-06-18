package com.ssafy.demo_app.api.routing;

import com.ssafy.demo_app.api.routing.dto.FactoryRoutingRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingStatusUpdateRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingTreeResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingUsageResponse;
import com.ssafy.demo_app.domain.routing.service.FactoryRoutingService;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FactoryRoutingController implements FactoryRoutingApi {

    private final FactoryRoutingService factoryRoutingService;

    @Override
    public ResponseEntity<ApiResponse<List<FactoryRoutingResponse>>> getRoutings(
            String factoryName,
            String lineName,
            FactoryRouting.RoutingStatus routingStatus
    ) {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getRoutings(
                factoryName,
                lineName,
                routingStatus
        )));
    }

    @Override
    public ResponseEntity<ApiResponse<FactoryRoutingResponse>> getRouting(Integer routingId) {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getRouting(routingId)));
    }

    @Override
    public ResponseEntity<ApiResponse<FactoryRoutingResponse>> createRouting(FactoryRoutingRequest request) {
        FactoryRoutingResponse response = factoryRoutingService.createRouting(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("라우팅이 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<FactoryRoutingResponse>> updateRouting(
            Integer routingId,
            FactoryRoutingRequest request
    ) {
        FactoryRoutingResponse response = factoryRoutingService.updateRouting(routingId, request);
        return ResponseEntity.ok(ApiResponse.success("라우팅이 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteRouting(Integer routingId) {
        factoryRoutingService.deleteRouting(routingId);
        return ResponseEntity.ok(ApiResponse.success("라우팅이 삭제되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<FactoryRoutingResponse>> updateRoutingStatus(
            Integer routingId,
            FactoryRoutingStatusUpdateRequest request
    ) {
        FactoryRoutingResponse response = factoryRoutingService.updateRoutingStatus(routingId, request);
        return ResponseEntity.ok(ApiResponse.success("라우팅 상태가 변경되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<FactoryRoutingUsageResponse>> getRoutingUsage(Integer routingId) {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getRoutingUsage(routingId)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<String>>> getFactories() {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getFactories()));
    }

    @Override
    public ResponseEntity<ApiResponse<List<String>>> getLines(String factoryName) {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getLines(factoryName)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<FactoryRoutingResponse>>> getOperations(
            String factoryName,
            String lineName
    ) {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getOperations(factoryName, lineName)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<FactoryRoutingTreeResponse>>> getRoutingTree() {
        return ResponseEntity.ok(ApiResponse.success(factoryRoutingService.getRoutingTree()));
    }
}

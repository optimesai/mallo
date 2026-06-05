package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationController implements LocationApi {

    private final InventoryService inventoryService;

    @Override
    public ResponseEntity<ApiResponse<PageResponse<LocationResponse>>> getLocations(Pageable pageable, String keyword) {
        PageResponse<LocationResponse> page = inventoryService.getLocations(pageable, keyword);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Override
    public ResponseEntity<ApiResponse<LocationResponse>> getLocation(Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLocation(id)));
    }

    @Override
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(LocationRequest request) {
        LocationResponse response = inventoryService.createLocation(request);
        return ResponseEntity.ok(ApiResponse.success("로케이션이 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(Integer id, LocationRequest request) {
        LocationResponse response = inventoryService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success("로케이션이 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteLocation(Integer id) {
        inventoryService.deleteLocation(id);
        return ResponseEntity.ok(ApiResponse.success("로케이션이 삭제되었습니다."));
    }
}

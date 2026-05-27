package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;
import com.ssafy.demo_app.domain.inventory.service.InventoryService;
import com.ssafy.demo_app.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getLocations() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLocations()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocation(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLocation(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(
            @Valid @RequestBody LocationRequest request
    ) {
        LocationResponse response = inventoryService.createLocation(request);
        return ResponseEntity.ok(ApiResponse.success("로케이션이 등록되었습니다.", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @PathVariable Integer id,
            @Valid @RequestBody LocationRequest request
    ) {
        LocationResponse response = inventoryService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success("로케이션이 수정되었습니다.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable Integer id) {
        inventoryService.deleteLocation(id);
        return ResponseEntity.ok(ApiResponse.success("로케이션이 삭제되었습니다."));
    }
}

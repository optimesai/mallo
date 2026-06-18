package com.ssafy.demo_app.api.inventory;

import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Warehouse Location API", description = "창고 로케이션(렉) CRUD 관리 API")
@RequestMapping("/api/locations")
public interface LocationApi {

    @Operation(summary = "로케이션 목록 조회", description = "등록된 창고 로케이션 목록을 페이징 및 키워드 검색으로 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<LocationResponse>>> getLocations(
            @PageableDefault(size = 20, sort = "locationCode") Pageable pageable,
            @Parameter(description = "로케이션 코드 또는 창고명 검색 키워드")
            @RequestParam(required = false) String keyword
    );

    @Operation(summary = "로케이션 단건 조회", description = "ID로 특정 로케이션 정보를 상세 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<LocationResponse>> getLocation(
            @Parameter(description = "로케이션 ID", required = true) @PathVariable Integer id
    );

    @Operation(summary = "로케이션 등록", description = "새로운 창고 로케이션을 등록합니다. 중복 코드는 사용 불가합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<LocationResponse>> createLocation(
            @Valid @RequestBody LocationRequest request
    );

    @Operation(summary = "로케이션 수정", description = "ID에 해당하는 로케이션 정보를 수정합니다. 중복 코드로 변경 불가합니다.")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @Parameter(description = "로케이션 ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody LocationRequest request
    );

    @Operation(summary = "로케이션 삭제", description = "특정 로케이션을 삭제합니다. 재고가 남아있는 경우 삭제 불가능합니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteLocation(
            @Parameter(description = "로케이션 ID", required = true) @PathVariable Integer id
    );
}

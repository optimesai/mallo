package com.ssafy.demo_app.api.routing;

import com.ssafy.demo_app.api.routing.dto.FactoryRoutingRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingStatusUpdateRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingTreeResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingUsageResponse;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Factory Routing API", description = "공장/라인/공정 라우팅 기준정보 관리 API")
@RequestMapping("/api/routings")
public interface FactoryRoutingApi {

    @Operation(summary = "라우팅 목록 조회", description = "공장명과 라인명으로 필터링하여 라우팅 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<List<FactoryRoutingResponse>>> getRoutings(
            @Parameter(description = "공장명 필터") @RequestParam(required = false) String factoryName,
            @Parameter(description = "라인명 필터") @RequestParam(required = false) String lineName,
            @Parameter(description = "라우팅 상태 필터") @RequestParam(required = false) FactoryRouting.RoutingStatus routingStatus
    );

    @Operation(summary = "라우팅 단건 조회", description = "ID로 특정 라우팅 정보를 조회합니다.")
    @GetMapping("/{routingId}")
    ResponseEntity<ApiResponse<FactoryRoutingResponse>> getRouting(
            @Parameter(description = "라우팅 ID", required = true) @PathVariable Integer routingId
    );

    @Operation(summary = "라우팅 등록", description = "공장명, 라인명, 공정 순서, 공정명을 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<FactoryRoutingResponse>> createRouting(
            @Valid @RequestBody FactoryRoutingRequest request
    );

    @Operation(summary = "라우팅 수정", description = "ID에 해당하는 라우팅 정보를 수정합니다.")
    @PutMapping("/{routingId}")
    ResponseEntity<ApiResponse<FactoryRoutingResponse>> updateRouting(
            @Parameter(description = "라우팅 ID", required = true) @PathVariable Integer routingId,
            @Valid @RequestBody FactoryRoutingRequest request
    );

    @Operation(summary = "라우팅 삭제", description = "작업지시에서 참조 중이지 않은 라우팅 정보를 삭제합니다.")
    @DeleteMapping("/{routingId}")
    ResponseEntity<ApiResponse<Void>> deleteRouting(
            @Parameter(description = "라우팅 ID", required = true) @PathVariable Integer routingId
    );

    @Operation(summary = "라우팅 상태 변경", description = "라우팅을 활성 또는 비활성 상태로 변경합니다.")
    @PatchMapping("/{routingId}/status")
    ResponseEntity<ApiResponse<FactoryRoutingResponse>> updateRoutingStatus(
            @Parameter(description = "라우팅 ID", required = true) @PathVariable Integer routingId,
            @Valid @RequestBody FactoryRoutingStatusUpdateRequest request
    );

    @Operation(summary = "라우팅 참조 현황 조회", description = "작업지시와 생산 실적에서 특정 라우팅을 참조하는 현황을 조회합니다.")
    @GetMapping("/{routingId}/usage")
    ResponseEntity<ApiResponse<FactoryRoutingUsageResponse>> getRoutingUsage(
            @Parameter(description = "라우팅 ID", required = true) @PathVariable Integer routingId
    );

    @Operation(summary = "공장 목록 조회", description = "라우팅에 등록된 공장명 목록을 조회합니다.")
    @GetMapping("/factories")
    ResponseEntity<ApiResponse<List<String>>> getFactories();

    @Operation(summary = "공장별 라인 목록 조회", description = "특정 공장에 등록된 라인명 목록을 조회합니다.")
    @GetMapping("/factories/{factoryName}/lines")
    ResponseEntity<ApiResponse<List<String>>> getLines(
            @Parameter(description = "공장명", required = true) @PathVariable String factoryName
    );

    @Operation(summary = "라인별 공정 목록 조회", description = "특정 공장/라인의 공정 목록을 순서대로 조회합니다.")
    @GetMapping("/factories/{factoryName}/lines/{lineName}/operations")
    ResponseEntity<ApiResponse<List<FactoryRoutingResponse>>> getOperations(
            @Parameter(description = "공장명", required = true) @PathVariable String factoryName,
            @Parameter(description = "라인명", required = true) @PathVariable String lineName
    );

    @Operation(summary = "라우팅 트리 조회", description = "공장 > 라인 > 공정 구조로 라우팅 정보를 조회합니다.")
    @GetMapping("/tree")
    ResponseEntity<ApiResponse<List<FactoryRoutingTreeResponse>>> getRoutingTree();
}

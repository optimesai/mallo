package com.ssafy.demo_app.api.production;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionCreateRequest;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Production Execution API", description = "생산 실적 관리 API")
@RequestMapping("/api/production-executions")
public interface ProductionExecutionApi {

    @Operation(summary = "작업 지시별 생산 실적 조회", description = "작업 지시 ID 또는 작업 지시 번호 기준으로 생산 실적 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<List<ProductionExecutionResponse>>> getExecutions(
            @Parameter(description = "작업 지시 ID 또는 작업 지시 번호", required = true) @RequestParam String orderKey
    );

    @Operation(summary = "생산 실적 등록", description = "RUN 상태 작업 지시에 실제 수행 라우팅, 양품, 불량, 공수를 등록합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ProductionExecutionResponse>> createExecution(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProductionExecutionCreateRequest request
    );

    @Operation(summary = "생산 실적 상세 조회", description = "생산 실적 ID로 단건 실적을 조회합니다.")
    @GetMapping("/{executionId}")
    ResponseEntity<ApiResponse<ProductionExecutionResponse>> getExecution(
            @Parameter(description = "생산 실적 ID", required = true) @PathVariable Integer executionId
    );

    @Operation(summary = "생산 실적 삭제", description = "마감되지 않은 작업 지시의 생산 실적을 삭제합니다.")
    @DeleteMapping("/{executionId}")
    ResponseEntity<ApiResponse<Void>> deleteExecution(
            @Parameter(description = "생산 실적 ID", required = true) @PathVariable Integer executionId
    );
}

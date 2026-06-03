package com.ssafy.demo_app.api.production;

import com.ssafy.demo_app.api.production.dto.ProductionExecutionCreateRequest;
import com.ssafy.demo_app.api.production.dto.ProductionExecutionResponse;
import com.ssafy.demo_app.domain.production.service.ProductionExecutionService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductionExecutionController implements ProductionExecutionApi {

    private final ProductionExecutionService productionExecutionService;

    @Override
    public ResponseEntity<ApiResponse<List<ProductionExecutionResponse>>> getExecutions(String orderKey) {
        return ResponseEntity.ok(ApiResponse.success(productionExecutionService.getExecutions(orderKey)));
    }

    @Override
    public ResponseEntity<ApiResponse<ProductionExecutionResponse>> createExecution(
            CustomUserDetails userDetails,
            ProductionExecutionCreateRequest request
    ) {
        ProductionExecutionResponse response = productionExecutionService.createExecution(userDetails.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("생산 실적이 등록되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<ProductionExecutionResponse>> getExecution(Integer executionId) {
        return ResponseEntity.ok(ApiResponse.success(productionExecutionService.getExecution(executionId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteExecution(Integer executionId) {
        productionExecutionService.deleteExecution(executionId);
        return ResponseEntity.ok(ApiResponse.success("생산 실적이 삭제되었습니다."));
    }
}

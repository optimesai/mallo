package com.ssafy.demo_app.api.production;

import com.ssafy.demo_app.api.production.dto.WorkOrderCloseRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderCreateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderDetailResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderStatusUpdateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderUpdateRequest;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.production.service.WorkOrderService;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.global.response.PageResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
@RestController
@RequiredArgsConstructor
public class WorkOrderController implements WorkOrderApi {

    private final WorkOrderService workOrderService;

    @Override
    public ResponseEntity<ApiResponse<WorkOrderResponse>> createWorkOrder(WorkOrderCreateRequest request) {
        WorkOrderResponse response = workOrderService.createWorkOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("작업 지시가 생성되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<PageResponse<WorkOrderResponse>>> getWorkOrders(
            Pageable pageable,
            WorkOrder.OrderStatus status,
            LocalDate planDate,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            String itemKeyword,
            String factoryName,
            String lineName,
            String operationName
    ) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getWorkOrders(
                pageable,
                status,
                planDate,
                fromDate,
                toDate,
                keyword,
                itemKeyword,
                factoryName,
                lineName,
                operationName
        )));
    }

    @Override
    public ResponseEntity<ApiResponse<WorkOrderDetailResponse>> getWorkOrder(String orderKey) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getWorkOrder(orderKey)));
    }

    @Override
    public ResponseEntity<ApiResponse<WorkOrderResponse>> updateWorkOrder(String orderKey, WorkOrderUpdateRequest request) {
        WorkOrderResponse response = workOrderService.updateWorkOrder(orderKey, request);
        return ResponseEntity.ok(ApiResponse.success("작업 지시가 수정되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteWorkOrder(String orderKey) {
        workOrderService.deleteWorkOrder(orderKey);
        return ResponseEntity.ok(ApiResponse.success("작업 지시가 삭제되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<WorkOrderResponse>> updateStatus(String orderKey, WorkOrderStatusUpdateRequest request) {
        WorkOrderResponse response = workOrderService.updateStatus(orderKey, request);
        return ResponseEntity.ok(ApiResponse.success("작업 지시 상태가 변경되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<WorkOrderResponse>> closeWorkOrder(String orderKey, WorkOrderCloseRequest request) {
        WorkOrderResponse response = workOrderService.closeWorkOrder(orderKey, request);
        return ResponseEntity.ok(ApiResponse.success("작업 지시가 마감되었습니다.", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> issueMaterials(
            CustomUserDetails userDetails,
            String orderKey
    ) {
        workOrderService.issueMaterials(orderKey, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("BOM 기반 생산 자재 출고 처리가 완료되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> cancelIssueMaterials(
            CustomUserDetails userDetails,
            String orderKey
    ) {
        workOrderService.cancelIssueMaterials(orderKey, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("BOM 기반 생산 자재 출고가 취소되었습니다."));
    }
}

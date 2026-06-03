package com.ssafy.demo_app.domain.production.service;

import com.ssafy.demo_app.api.production.dto.WorkOrderCloseRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderCreateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderDetailResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderResponse;
import com.ssafy.demo_app.api.production.dto.WorkOrderStatusUpdateRequest;
import com.ssafy.demo_app.api.production.dto.WorkOrderUpdateRequest;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;

import java.time.LocalDate;
import java.util.List;

public interface WorkOrderService {
    WorkOrderResponse createWorkOrder(WorkOrderCreateRequest request);
    List<WorkOrderResponse> getWorkOrders(
            WorkOrder.OrderStatus status,
            LocalDate planDate,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            String factoryName,
            String lineName
    );
    WorkOrderDetailResponse getWorkOrder(String orderKey);
    WorkOrderResponse updateWorkOrder(String orderKey, WorkOrderUpdateRequest request);
    void deleteWorkOrder(String orderKey);
    WorkOrderResponse updateStatus(String orderKey, WorkOrderStatusUpdateRequest request);
    WorkOrderResponse closeWorkOrder(String orderKey, WorkOrderCloseRequest request);
    void issueMaterials(String orderKey, Integer workerId);
}

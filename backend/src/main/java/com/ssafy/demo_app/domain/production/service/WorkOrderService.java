package com.ssafy.demo_app.domain.production.service;

public interface WorkOrderService {
    void issueMaterials(Integer orderId, Integer workerId);
}

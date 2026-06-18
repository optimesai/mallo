package com.ssafy.demo_app.domain.routing.service;

import com.ssafy.demo_app.api.routing.dto.FactoryRoutingRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingStatusUpdateRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingTreeResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingUsageResponse;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;

import java.util.List;

public interface FactoryRoutingService {

    List<FactoryRoutingResponse> getRoutings(
            String factoryName,
            String lineName,
            FactoryRouting.RoutingStatus routingStatus
    );

    FactoryRoutingResponse getRouting(Integer routingId);

    FactoryRoutingResponse createRouting(FactoryRoutingRequest request);

    FactoryRoutingResponse updateRouting(Integer routingId, FactoryRoutingRequest request);

    void deleteRouting(Integer routingId);

    FactoryRoutingResponse updateRoutingStatus(Integer routingId, FactoryRoutingStatusUpdateRequest request);

    FactoryRoutingUsageResponse getRoutingUsage(Integer routingId);

    List<String> getFactories();

    List<String> getLines(String factoryName);

    List<FactoryRoutingResponse> getOperations(String factoryName, String lineName);

    List<FactoryRoutingTreeResponse> getRoutingTree();
}

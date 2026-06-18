package com.ssafy.demo_app.api.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FactoryRoutingUsageResponse {

    private Integer routingId;
    private long workOrderCount;
    private long executionCount;
    private List<String> workOrderNos;
    private List<Integer> executionIds;
    private boolean canUpdate;
    private boolean canDelete;
    private String recommendedAction;
}

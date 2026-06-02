package com.ssafy.demo_app.api.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FactoryRoutingTreeResponse {

    private String factoryName;
    private List<LineResponse> lines;

    @Getter
    @AllArgsConstructor
    public static class LineResponse {
        private String lineName;
        private List<OperationResponse> operations;
    }

    @Getter
    @AllArgsConstructor
    public static class OperationResponse {
        private Integer routingId;
        private Integer operationSeq;
        private String operationName;
    }
}

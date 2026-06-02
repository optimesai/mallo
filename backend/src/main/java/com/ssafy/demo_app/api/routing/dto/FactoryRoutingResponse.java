package com.ssafy.demo_app.api.routing.dto;

import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "공장/라인/공정 라우팅 응답 객체")
public class FactoryRoutingResponse {

    private Integer routingId;
    private String factoryName;
    private String lineName;
    private Integer operationSeq;
    private String operationName;
    private LocalDateTime createdAt;

    public static FactoryRoutingResponse from(FactoryRouting routing) {
        FactoryRoutingResponse response = new FactoryRoutingResponse();
        response.setRoutingId(routing.getRoutingId());
        response.setFactoryName(routing.getFactoryName());
        response.setLineName(routing.getLineName());
        response.setOperationSeq(routing.getOperationSeq());
        response.setOperationName(routing.getOperationName());
        response.setCreatedAt(routing.getCreatedAt());
        return response;
    }
}

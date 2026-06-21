package com.ssafy.demo_app.api.production.dto;

import lombok.Getter;

@Getter
public class WorkOrderOperationProgressResponse {

    private Integer routingId;
    private String factoryName;
    private String lineName;
    private Integer operationSeq;
    private String operationName;
    private Integer targetQty;
    private Integer availableQty;
    private Integer completedGoodQty;
    private Integer completedDefectQty;
    private Integer completedQty;
    private Boolean currentOperation;
    private Boolean completed;

    public WorkOrderOperationProgressResponse(
            Integer routingId,
            String factoryName,
            String lineName,
            Integer operationSeq,
            String operationName,
            Integer targetQty,
            Integer availableQty,
            Integer completedGoodQty,
            Integer completedDefectQty,
            Boolean currentOperation
    ) {
        this.routingId = routingId;
        this.factoryName = factoryName;
        this.lineName = lineName;
        this.operationSeq = operationSeq;
        this.operationName = operationName;
        this.targetQty = targetQty;
        this.availableQty = availableQty;
        this.completedGoodQty = completedGoodQty;
        this.completedDefectQty = completedDefectQty;
        this.completedQty = completedGoodQty + completedDefectQty;
        this.currentOperation = currentOperation;
        this.completed = this.completedQty >= availableQty && availableQty > 0;
    }
}

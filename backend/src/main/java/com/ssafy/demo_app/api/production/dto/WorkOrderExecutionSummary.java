package com.ssafy.demo_app.api.production.dto;

public record WorkOrderExecutionSummary(
        int totalGoodQty,
        int totalDefectQty,
        int totalManHoursMinutes
) {
    public static WorkOrderExecutionSummary empty() {
        return new WorkOrderExecutionSummary(0, 0, 0);
    }
}

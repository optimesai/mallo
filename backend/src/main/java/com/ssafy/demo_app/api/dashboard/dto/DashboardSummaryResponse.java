package com.ssafy.demo_app.api.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DashboardSummaryResponse {

    private LocalDateTime generatedAt;
    private String period;
    private List<DashboardSummaryCardResponse> summaryCards;
    private List<DashboardMetricViewResponse> metricViews;
    private List<DashboardInsightResponse> insights;
}

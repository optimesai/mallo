package com.ssafy.demo_app.api.dashboard.dto;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DashboardMetricViewResponse {

    private String id;
    private String label;
    private String title;
    private String subtitle;
    private AiChartResponse chart;
    private List<Map<String, Object>> rows;
}

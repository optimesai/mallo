package com.ssafy.demo_app.domain.ai.service;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChartSpecValidationResult {

    private boolean valid;
    private String message;
    private AiChartResponse chart;

    public static ChartSpecValidationResult valid(AiChartResponse chart) {
        ChartSpecValidationResult result = new ChartSpecValidationResult();
        result.setValid(true);
        result.setMessage("success");
        result.setChart(chart);
        return result;
    }

    public static ChartSpecValidationResult invalid(String message) {
        ChartSpecValidationResult result = new ChartSpecValidationResult();
        result.setValid(false);
        result.setMessage(message);
        return result;
    }
}

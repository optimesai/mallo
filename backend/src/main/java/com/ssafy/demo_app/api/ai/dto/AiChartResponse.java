package com.ssafy.demo_app.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiChartResponse {

    private Boolean enabled;
    private ChartType type;
    @JsonProperty("xKey")
    private String xKey;
    @JsonProperty("yKeys")
    private List<String> yKeys;
    private String xLabel;
    @JsonProperty("yLabels")
    private Map<String, String> yLabels;
    private String labelKey;
    private String labelFormat;
    private String title;
    private String reason;

    public static AiChartResponse none(String reason) {
        AiChartResponse response = new AiChartResponse();
        response.setEnabled(false);
        response.setType(ChartType.NONE);
        response.setYKeys(List.of());
        response.setYLabels(Map.of());
        response.setReason(reason);
        return response;
    }

    public static AiChartResponse table(String title, String reason) {
        AiChartResponse response = new AiChartResponse();
        response.setEnabled(true);
        response.setType(ChartType.TABLE);
        response.setYKeys(List.of());
        response.setYLabels(Map.of());
        response.setTitle(title);
        response.setReason(reason);
        return response;
    }

    public enum ChartType {
        NONE,
        TABLE,
        STAT,
        BAR,
        LINE,
        DONUT,
        HORIZONTAL_BAR,
        STACKED_BAR,
        AREA,
        COMBO,
        PARETO
    }
}

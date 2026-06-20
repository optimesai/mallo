package com.ssafy.demo_app.domain.ai.service.chart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.domain.ai.service.assistant.ChartRecommendationGenerator;
import com.ssafy.demo_app.domain.ai.service.chart.ChartSpecValidationService.ChartSpecValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChartRecommendationServiceImpl implements ChartRecommendationService {

    private static final String NO_CHART_REASON = "차트로 표현할 수 있는 데이터가 없습니다.";
    private static final String INVALID_CHART_REASON = "차트 추천 결과가 데이터 구조와 일치하지 않습니다.";

    private final ChartRecommendationGenerator chartRecommendationGenerator;
    private final ChartSpecValidationService chartSpecValidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiChartResponse recommend(String question, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return AiChartResponse.none(NO_CHART_REASON);
        }

        String rowsJson = toJson(rows);
        try {
            String rawChartSpec = chartRecommendationGenerator.recommend(question, rowsJson);
            AiChartResponse chart = objectMapper.readValue(sanitize(rawChartSpec), AiChartResponse.class);
            ChartSpecValidationResult validation = chartSpecValidationService.validate(chart, rows);

            if (!validation.isValid()) {
                return AiChartResponse.none(validation.getMessage());
            }

            return validation.getChart();
        } catch (Exception exception) {
            return AiChartResponse.none(INVALID_CHART_REASON + " " + exception.getMessage());
        }
    }

    private String toJson(List<Map<String, Object>> rows) {
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private String sanitize(String chartSpec) {
        if (chartSpec == null) {
            return "{}";
        }

        return chartSpec
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}

package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;

import java.util.List;
import java.util.Map;

public interface ChartRecommendationService {

    AiChartResponse recommend(String question, List<Map<String, Object>> rows);
}

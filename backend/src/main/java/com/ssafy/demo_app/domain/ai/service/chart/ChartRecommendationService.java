package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.domain.ai.service.classification.AiIntentResult;

import java.util.List;
import java.util.Map;

public interface ChartRecommendationService {

    AiChartResponse recommend(String question, AiIntentResult intentResult, List<Map<String, Object>> rows);
}

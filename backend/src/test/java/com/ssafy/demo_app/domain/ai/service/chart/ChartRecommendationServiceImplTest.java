package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChartRecommendationServiceImplTest {

    private final ChartSpecValidationService chartSpecValidationService = new ChartSpecValidationService();

    @Test
    void recommend_returnsValidChartWhenGeneratorReturnsValidJson() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> """
                        {
                          "enabled": true,
                          "type": "BAR",
                          "xKey": "item_name",
                          "yKeys": ["quantity"],
                          "title": "품목별 입고 수량",
                          "reason": "품목별 수량 비교에 적합합니다."
                        }
                        """,
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "품목별 입고 수량을 보여줘",
                List.of(Map.of("item_name", "A품목", "quantity", 10))
        );

        assertThat(chart.getEnabled()).as(chart.getReason()).isTrue();
        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.BAR);
        assertThat(chart.getXKey()).isEqualTo("item_name");
    }

    @Test
    void recommend_returnsNoneWhenGeneratorReturnsInvalidJson() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> "not-json",
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "품목별 입고 수량을 보여줘",
                List.of(Map.of("item_name", "A품목", "quantity", 10))
        );

        assertThat(chart.getEnabled()).isFalse();
        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.NONE);
    }

    @Test
    void recommend_returnsNoneWhenChartSpecDoesNotMatchRows() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> """
                        {
                          "enabled": true,
                          "type": "BAR",
                          "xKey": "itemName",
                          "yKeys": ["quantity"],
                          "title": "품목별 입고 수량",
                          "reason": "품목별 수량 비교에 적합합니다."
                        }
                        """,
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "품목별 입고 수량을 보여줘",
                List.of(Map.of("item_name", "A품목", "quantity", 10))
        );

        assertThat(chart.getEnabled()).isFalse();
        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.NONE);
    }

    @Test
    void recommend_returnsNoneWhenRowsAreEmpty() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> {
                    throw new AssertionError("Generator must not be called for empty rows");
                },
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend("입고 수량을 보여줘", List.of());

        assertThat(chart.getEnabled()).isFalse();
        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.NONE);
    }
}

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
                List.of(
                        Map.of("item_name", "A품목", "quantity", 10),
                        Map.of("item_name", "B품목", "quantity", 20)
                )
        );

        assertThat(chart.getEnabled()).as(chart.getReason()).isTrue();
        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.BAR);
        assertThat(chart.getXKey()).isEqualTo("item_name");
    }

    @Test
    void recommend_returnsTableForRoutingList() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> {
                    throw new AssertionError("Generator must not be called for clear table presentation");
                },
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "라우팅 보여줘",
                List.of(Map.of("routing_id", 1, "factory_name", "1공장", "line_name", "A라인", "operation_seq", 10))
        );

        assertThat(chart.getEnabled()).isTrue();
        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.TABLE);
    }

    @Test
    void recommend_returnsLineForTrendQuestion() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> {
                    throw new AssertionError("Generator must not be called for clear line presentation");
                },
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "최근 7일 입고 수량 추이",
                List.of(
                        Map.of("inbound_date", "2026-06-17", "total_qty", 10),
                        Map.of("inbound_date", "2026-06-18", "total_qty", 20)
                )
        );

        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.LINE);
        assertThat(chart.getXKey()).isEqualTo("inbound_date");
    }

    @Test
    void recommend_returnsDonutForStatusDistribution() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> {
                    throw new AssertionError("Generator must not be called for clear donut presentation");
                },
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "작업지시 상태별 건수 분포",
                List.of(
                        Map.of("status", "READY", "order_count", 10),
                        Map.of("status", "RUN", "order_count", 20)
                )
        );

        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.DONUT);
        assertThat(chart.getXKey()).isEqualTo("status");
    }

    @Test
    void recommend_returnsStatForSingleKpi() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> {
                    throw new AssertionError("Generator must not be called for clear stat presentation");
                },
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "총 출하 대기 건수",
                List.of(Map.of("total_count", 12))
        );

        assertThat(chart.getType()).isEqualTo(AiChartResponse.ChartType.STAT);
        assertThat(chart.getYKeys()).isEqualTo(List.of("total_count"));
    }

    @Test
    void recommend_returnsNoneWhenGeneratorReturnsInvalidJson() {
        ChartRecommendationServiceImpl service = new ChartRecommendationServiceImpl(
                (question, rowsJson) -> "not-json",
                chartSpecValidationService
        );

        AiChartResponse chart = service.recommend(
                "시각화 추천해줘",
                List.of(
                        Map.of("item_name", "A품목", "quantity", 10),
                        Map.of("item_name", "B품목", "quantity", 20)
                )
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
                "시각화 추천해줘",
                List.of(
                        Map.of("item_name", "A품목", "quantity", 10),
                        Map.of("item_name", "B품목", "quantity", 20)
                )
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

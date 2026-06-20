package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.domain.ai.service.chart.ChartSpecValidationService.ChartSpecValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChartSpecValidationServiceTest {

    private final ChartSpecValidationService chartSpecValidationService = new ChartSpecValidationService();

    @Test
    void validate_acceptsBarWithExistingXKeyAndNumericYKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.BAR, "item_name", List.of("quantity"));
        List<Map<String, Object>> rows = List.of(
                Map.of("item_name", "A품목", "quantity", 10),
                Map.of("item_name", "B품목", "quantity", 20)
        );

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getChart().getEnabled()).isTrue();
    }

    @Test
    void validate_acceptsLineWithExistingXKeyAndNumericYKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.LINE, "inbound_date", List.of("quantity"));
        List<Map<String, Object>> rows = List.of(
                Map.of("inbound_date", "2026-06-17", "quantity", 10),
                Map.of("inbound_date", "2026-06-18", "quantity", 20)
        );

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_acceptsDonutWithOneYKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.DONUT, "status", List.of("count"));
        List<Map<String, Object>> rows = List.of(
                Map.of("status", "READY", "count", 10),
                Map.of("status", "COMPLETED", "count", 20)
        );

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_acceptsStatWithOneNumericYKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.STAT, null, List.of("total_count"));
        List<Map<String, Object>> rows = List.of(Map.of("total_count", 10));

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getChart().getXKey()).isNull();
    }

    @Test
    void validate_blocksMissingXKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.BAR, "missing", List.of("quantity"));
        List<Map<String, Object>> rows = List.of(Map.of("item_name", "A품목", "quantity", 10));

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validate_blocksMissingYKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.BAR, "item_name", List.of("missing"));
        List<Map<String, Object>> rows = List.of(Map.of("item_name", "A품목", "quantity", 10));

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validate_blocksNonNumericYKey() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.BAR, "item_name", List.of("quantity"));
        List<Map<String, Object>> rows = List.of(Map.of("item_name", "A품목", "quantity", "10"));

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validate_blocksDonutWithMultipleYKeys() {
        AiChartResponse chart = chart(AiChartResponse.ChartType.DONUT, "status", List.of("count", "quantity"));
        List<Map<String, Object>> rows = List.of(Map.of("status", "READY", "count", 10, "quantity", 20));

        ChartSpecValidationResult result = chartSpecValidationService.validate(chart, rows);

        assertThat(result.isValid()).isFalse();
    }

    private AiChartResponse chart(AiChartResponse.ChartType type, String xKey, List<String> yKeys) {
        AiChartResponse chart = new AiChartResponse();
        chart.setEnabled(true);
        chart.setType(type);
        chart.setXKey(xKey);
        chart.setYKeys(yKeys);
        chart.setTitle("테스트 차트");
        chart.setReason("테스트");
        return chart;
    }
}

package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChartSpecValidationService {

    private static final String INVALID_CHART = "차트 추천 결과가 데이터 구조와 일치하지 않습니다.";
    private static final String NO_CHART = "차트로 표현할 수 있는 데이터가 없습니다.";

    public ChartSpecValidationResult validate(AiChartResponse chart, List<Map<String, Object>> rows) {
        if (chart == null || chart.getType() == null) {
            return ChartSpecValidationResult.invalid(INVALID_CHART);
        }
        if (rows == null || rows.isEmpty()) {
            if (chart.getType() == AiChartResponse.ChartType.NONE) {
                return ChartSpecValidationResult.valid(normalizeNone(chart, NO_CHART));
            }
            return ChartSpecValidationResult.invalid(NO_CHART);
        }
        if (chart.getType() == AiChartResponse.ChartType.NONE) {
            return ChartSpecValidationResult.valid(normalizeNone(chart, chart.getReason()));
        }

        List<String> yKeys = chart.getYKeys();
        if (yKeys == null || yKeys.isEmpty()) {
            return ChartSpecValidationResult.invalid("숫자형 y축 컬럼이 필요합니다.");
        }
        if (!validateYKeys(yKeys, rows)) {
            return ChartSpecValidationResult.invalid("y축 컬럼은 실제 결과에 존재하는 숫자형 컬럼이어야 합니다.");
        }

        return switch (chart.getType()) {
            case STAT -> validateStat(chart);
            case BAR, LINE -> validateAxisChart(chart, rows);
            case DONUT -> validateDonut(chart, rows);
            case NONE -> ChartSpecValidationResult.valid(normalizeNone(chart, chart.getReason()));
        };
    }

    private ChartSpecValidationResult validateStat(AiChartResponse chart) {
        if (chart.getYKeys().size() != 1) {
            return ChartSpecValidationResult.invalid("STAT 차트는 하나의 숫자형 컬럼만 사용할 수 있습니다.");
        }

        chart.setEnabled(true);
        chart.setXKey(null);
        return ChartSpecValidationResult.valid(chart);
    }

    private ChartSpecValidationResult validateAxisChart(AiChartResponse chart, List<Map<String, Object>> rows) {
        if (!hasColumn(chart.getXKey(), rows)) {
            return ChartSpecValidationResult.invalid("x축 컬럼이 실제 결과에 존재해야 합니다.");
        }
        if (chart.getYKeys().size() > 2) {
            return ChartSpecValidationResult.invalid("BAR/LINE 차트는 최대 두 개의 y축 컬럼만 사용할 수 있습니다.");
        }

        chart.setEnabled(true);
        return ChartSpecValidationResult.valid(chart);
    }

    private ChartSpecValidationResult validateDonut(AiChartResponse chart, List<Map<String, Object>> rows) {
        if (!hasColumn(chart.getXKey(), rows)) {
            return ChartSpecValidationResult.invalid("DONUT 차트는 실제 결과에 존재하는 라벨 컬럼이 필요합니다.");
        }
        if (chart.getYKeys().size() != 1) {
            return ChartSpecValidationResult.invalid("DONUT 차트는 하나의 숫자형 컬럼만 사용할 수 있습니다.");
        }

        chart.setEnabled(true);
        return ChartSpecValidationResult.valid(chart);
    }

    private boolean validateYKeys(List<String> yKeys, List<Map<String, Object>> rows) {
        for (String yKey : yKeys) {
            if (!hasColumn(yKey, rows) || !isNumericColumn(yKey, rows)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasColumn(String key, List<Map<String, Object>> rows) {
        return key != null
                && !key.isBlank()
                && rows.stream().anyMatch(row -> row.containsKey(key));
    }

    private boolean isNumericColumn(String key, List<Map<String, Object>> rows) {
        boolean hasNumericValue = false;

        for (Map<String, Object> row : rows) {
            Object value = row.get(key);
            if (value == null) {
                continue;
            }
            if (!(value instanceof Number)) {
                return false;
            }
            hasNumericValue = true;
        }

        return hasNumericValue;
    }

    private AiChartResponse normalizeNone(AiChartResponse chart, String reason) {
        chart.setEnabled(false);
        chart.setType(AiChartResponse.ChartType.NONE);
        chart.setXKey(null);
        chart.setYKeys(List.of());
        chart.setTitle(null);
        chart.setReason(reason == null || reason.isBlank() ? NO_CHART : reason);
        return chart;
    }

    @Getter
    @Setter
    public static class ChartSpecValidationResult {

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
}

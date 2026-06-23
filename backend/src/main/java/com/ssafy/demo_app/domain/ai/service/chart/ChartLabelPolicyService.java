package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.domain.ai.service.classification.AiIntentResult;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChartLabelPolicyService {

    public AiChartResponse apply(AiChartResponse chart, String question, AiIntentResult intentResult, List<String> columns) {
        if (chart == null) {
            return null;
        }
        if (chart.getYKeys() == null) {
            chart.setYKeys(List.of());
        }
        if (chart.getYLabels() == null || chart.getYLabels().isEmpty()) {
            chart.setYLabels(resolveYLabels(chart.getYKeys()));
        }
        if (chart.getXLabel() == null || chart.getXLabel().isBlank()) {
            chart.setXLabel(resolveAxisLabel(chart.getXKey()));
        }
        if (chart.getLabelKey() == null || chart.getLabelKey().isBlank()) {
            chart.setLabelKey(resolvePreferredLabelKey(columns, chart.getXKey()));
        }
        if (chart.getLabelFormat() == null) {
            chart.setLabelFormat(resolveLabelFormat(chart.getLabelKey()));
        }
        if (chart.getTitle() == null || chart.getTitle().isBlank()) {
            chart.setTitle(resolveTitle(question, intentResult, chart));
        }
        return chart;
    }

    public String resolvePreferredLabelKey(List<String> columns, String fallbackKey) {
        for (String key : List.of("item_label", "partner_label", "operation_label", "line_label", "warehouse_label")) {
            if (columns.contains(key)) {
                return key;
            }
        }
        return fallbackKey;
    }

    private Map<String, String> resolveYLabels(List<String> yKeys) {
        Map<String, String> labels = new LinkedHashMap<>();
        yKeys.forEach(key -> labels.put(key, resolveAxisLabel(key)));
        return labels;
    }

    private String resolveTitle(String question, AiIntentResult intentResult, AiChartResponse chart) {
        String metricLabel = resolveMetricLabel(intentResult);
        if (!metricLabel.isBlank()) {
            return metricLabel + " 분석";
        }
        if (question != null && !question.isBlank()) {
            return question.length() > 28 ? question.substring(0, 28) + "..." : question;
        }
        if (chart.getType() == AiChartResponse.ChartType.TABLE) {
            return "표 형식 조회";
        }
        return "데이터 시각화";
    }

    private String resolveMetricLabel(AiIntentResult intentResult) {
        if (intentResult == null || intentResult.getMetric() == null) {
            return "";
        }
        return switch (normalize(intentResult.getMetric())) {
            case "current_stock" -> "현재고";
            case "safety_stock_shortage" -> "안전재고 부족";
            case "inbound_qty" -> "입고 수량";
            case "shipping_waiting_qty" -> "출하 대기";
            case "production_qty" -> "생산 실적";
            case "defect_rate" -> "불량률";
            case "work_order_progress" -> "작업 지시 진행";
            case "operation_issue" -> "운영 이슈";
            default -> "";
        };
    }

    private String resolveAxisLabel(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        return switch (normalize(key)) {
            case "item_label" -> "품목";
            case "partner_label" -> "거래처";
            case "operation_label" -> "공장 / 라인 / 공정";
            case "line_name", "line_label" -> "라인";
            case "operation_name" -> "공정";
            case "warehouse_name", "warehouse_label" -> "창고";
            case "period_date" -> "일자";
            case "period_month" -> "월";
            case "period_week" -> "주";
            case "current_qty" -> "현재고 수량";
            case "safety_stock" -> "안전재고";
            case "shortage_qty" -> "부족 수량";
            case "inbound_qty", "total_inbound_qty" -> "입고 수량";
            case "waiting_qty" -> "출하 대기 수량";
            case "production_qty" -> "생산 수량";
            case "good_qty" -> "양품 수량";
            case "defect_qty" -> "불량 수량";
            case "defect_rate" -> "불량률";
            case "order_count", "work_order_count" -> "작업 지시 건수";
            case "count", "total_count" -> "건수";
            default -> key;
        };
    }

    private String resolveLabelFormat(String labelKey) {
        if (labelKey == null) {
            return "";
        }
        return switch (normalize(labelKey)) {
            case "item_label" -> "품목코드 / 품목명";
            case "partner_label" -> "거래처코드 / 거래처명";
            case "operation_label" -> "공장, 라인, 공정을 줄바꿈으로 표시";
            default -> "";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}

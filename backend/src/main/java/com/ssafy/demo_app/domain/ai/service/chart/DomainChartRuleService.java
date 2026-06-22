package com.ssafy.demo_app.domain.ai.service.chart;

import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.domain.ai.service.classification.AiIntentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DomainChartRuleService {

    private final ChartLabelPolicyService chartLabelPolicyService;

    public Optional<AiChartResponse> recommend(String question, AiIntentResult intentResult, List<String> columns, List<String> numericColumns) {
        if (numericColumns.isEmpty()) {
            return Optional.empty();
        }

        String domain = normalize(intentResult == null ? "" : intentResult.getDomain());
        String intent = normalize(intentResult == null ? "" : intentResult.getIntent());
        String metric = normalize(intentResult == null ? "" : intentResult.getMetric());
        String normalizedQuestion = normalize(question);
        if (domain.isBlank() && intent.isBlank() && metric.isBlank()) {
            return Optional.empty();
        }
        Optional<String> dateColumn = columns.stream().filter(this::isDateLikeColumn).findFirst();
        Optional<String> labelColumn = resolveLabelColumn(columns);

        if (isTrend(intent, normalizedQuestion) && dateColumn.isPresent()) {
            AiChartResponse.ChartType type = isVolumeMetric(metric) ? AiChartResponse.ChartType.AREA : AiChartResponse.ChartType.LINE;
            return Optional.of(axis(type, dateColumn.get(), List.of(numericColumns.get(0)), "기간별 추이", "기간 흐름을 확인하는 질의는 추이형 차트가 적합합니다.", question, intentResult, columns));
        }

        if (metric.equals("defect_rate") && hasAny(normalizedQuestion, List.of("원인", "사유", "이유"))) {
            return labelColumn.map(column -> axis(AiChartResponse.ChartType.PARETO, column, List.of(numericColumns.get(0)), "불량 원인 파레토", "불량 원인별 영향도를 누적 비율과 함께 확인합니다.", question, intentResult, columns));
        }

        if (metric.equals("defect_rate") && numericColumns.size() >= 2 && hasAny(columns, List.of("production_qty", "total_qty", "defect_rate"))) {
            return labelColumn.map(column -> axis(AiChartResponse.ChartType.COMBO, column, numericColumns.stream().limit(2).toList(), "생산량과 불량률", "수량과 비율을 함께 비교해야 하므로 COMBO 차트가 적합합니다.", question, intentResult, columns));
        }

        if (domain.equals("production") && hasAny(columns, List.of("good_qty", "defect_qty"))) {
            return labelColumn.map(column -> axis(AiChartResponse.ChartType.STACKED_BAR, column, numericColumns.stream().limit(3).toList(), "생산 실적 구성", "양품과 불량 수량 구성을 누적으로 비교합니다.", question, intentResult, columns));
        }

        if ((domain.equals("shipping") || domain.equals("inbound")) && hasAny(normalizedQuestion, List.of("상태별", "상태"))) {
            return labelColumn.map(column -> axis(AiChartResponse.ChartType.STACKED_BAR, column, numericColumns.stream().limit(3).toList(), "상태별 물량 구성", "상태별 물량은 누적 막대로 비교하기 적합합니다.", question, intentResult, columns));
        }

        if (isRankingOrComparison(intent, normalizedQuestion) && labelColumn.isPresent()) {
            AiChartResponse.ChartType type = hasLongBusinessLabel(columns) ? AiChartResponse.ChartType.HORIZONTAL_BAR : AiChartResponse.ChartType.BAR;
            return Optional.of(axis(type, labelColumn.get(), numericColumns.stream().limit(2).toList(), "항목별 비교", "업무 항목별 지표 비교는 막대 차트가 적합합니다.", question, intentResult, columns));
        }

        if ((domain.equals("inventory") || metric.equals("safety_stock_shortage")) && labelColumn.isPresent()) {
            return Optional.of(axis(AiChartResponse.ChartType.HORIZONTAL_BAR, labelColumn.get(), List.of(numericColumns.get(0)), "재고 위험 항목", "품목 라벨이 긴 재고 위험 항목은 가로 막대 차트가 적합합니다.", question, intentResult, columns));
        }

        return Optional.empty();
    }

    private AiChartResponse axis(
            AiChartResponse.ChartType type,
            String xKey,
            List<String> yKeys,
            String title,
            String reason,
            String question,
            AiIntentResult intentResult,
            List<String> columns
    ) {
        AiChartResponse response = new AiChartResponse();
        response.setEnabled(true);
        response.setType(type);
        response.setXKey(xKey);
        response.setYKeys(yKeys);
        response.setTitle(title);
        response.setReason(reason);
        return chartLabelPolicyService.apply(response, question, intentResult, columns);
    }

    private Optional<String> resolveLabelColumn(List<String> columns) {
        for (String key : List.of("item_label", "partner_label", "operation_label", "line_name", "operation_name", "warehouse_name", "status", "issue_type", "defect_reason", "reason_text")) {
            if (columns.contains(key)) {
                return Optional.of(key);
            }
        }
        return columns.stream().filter(column -> !isDateLikeColumn(column)).findFirst();
    }

    private boolean isTrend(String intent, String question) {
        return intent.equals("trend") || hasAny(question, List.of("추이", "트렌드", "일별", "월별", "주별"));
    }

    private boolean isRankingOrComparison(String intent, String question) {
        return intent.equals("ranking")
                || intent.equals("comparison")
                || hasAny(question, List.of("비교", "순위", "상위", "하위", "별", "랭킹", "top"));
    }

    private boolean isVolumeMetric(String metric) {
        return List.of("current_stock", "inbound_qty", "shipping_waiting_qty", "production_qty").contains(metric);
    }

    private boolean hasLongBusinessLabel(List<String> columns) {
        return hasAny(columns, List.of("item_label", "partner_label", "operation_label", "item_name", "partner_name", "operation_name"));
    }

    private boolean isDateLikeColumn(String key) {
        String normalizedKey = normalize(key);
        return normalizedKey.contains("date")
                || normalizedKey.contains("day")
                || normalizedKey.contains("month")
                || normalizedKey.contains("week")
                || normalizedKey.contains("year");
    }

    private boolean hasAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private boolean hasAny(List<String> values, List<String> keywords) {
        return values.stream().map(this::normalize).anyMatch(value -> keywords.stream().anyMatch(value::contains));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}

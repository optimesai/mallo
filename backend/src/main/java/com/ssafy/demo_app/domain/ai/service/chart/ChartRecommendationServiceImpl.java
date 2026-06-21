package com.ssafy.demo_app.domain.ai.service.chart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
import com.ssafy.demo_app.domain.ai.service.assistant.ChartRecommendationGenerator;
import com.ssafy.demo_app.domain.ai.service.chart.ChartSpecValidationService.ChartSpecValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChartRecommendationServiceImpl implements ChartRecommendationService {

    private static final String NO_CHART_REASON = "차트로 표현할 수 있는 데이터가 없습니다.";
    private static final String INVALID_CHART_REASON = "차트 추천 결과가 데이터 구조와 일치하지 않습니다.";
    private static final Set<String> TABLE_QUESTION_KEYWORDS = Set.of(
            "라우팅",
            "목록",
            "리스트",
            "상세",
            "마스터",
            "조회",
            "보여줘",
            "알려줘"
    );
    private static final Set<String> TREND_QUESTION_KEYWORDS = Set.of(
            "추이",
            "트렌드",
            "일별",
            "월별",
            "주별",
            "변화",
            "최근"
    );
    private static final Set<String> RATIO_QUESTION_KEYWORDS = Set.of(
            "비중",
            "점유율",
            "분포",
            "구성",
            "상태별",
            "유형별"
    );
    private static final Set<String> COMPARISON_QUESTION_KEYWORDS = Set.of(
            "비교",
            "순위",
            "상위",
            "하위",
            "별",
            "랭킹"
    );
    private static final Set<String> NON_METRIC_KEYWORDS = Set.of(
            "id",
            "_id",
            "seq",
            "no",
            "code",
            "status"
    );

    private final ChartRecommendationGenerator chartRecommendationGenerator;
    private final ChartSpecValidationService chartSpecValidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiChartResponse recommend(String question, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return AiChartResponse.none(NO_CHART_REASON);
        }

        Optional<AiChartResponse> ruleBasedChart = recommendByRules(question, rows);
        if (ruleBasedChart.isPresent()) {
            return ruleBasedChart.get();
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

    private Optional<AiChartResponse> recommendByRules(String question, List<Map<String, Object>> rows) {
        List<String> columns = resolveColumns(rows);
        List<String> numericColumns = columns.stream()
                .filter(column -> isNumericColumn(column, rows))
                .filter(column -> !isIdentifierLikeColumn(column))
                .toList();
        List<String> labelColumns = columns.stream()
                .filter(column -> !numericColumns.contains(column))
                .toList();
        String normalizedQuestion = normalize(question);

        if (numericColumns.isEmpty()) {
            return Optional.of(AiChartResponse.table(
                    "표 형식 조회",
                    "숫자 지표가 없는 목록성 데이터는 표 형식이 가장 적합합니다."
            ));
        }

        if (isTableQuestion(normalizedQuestion) && !isAnalyticalQuestion(normalizedQuestion)) {
            return Optional.of(AiChartResponse.table(
                    "표 형식 조회",
                    "목록, 상세, 라우팅 조회는 그래프보다 표 형식이 더 적합합니다."
            ));
        }

        if (rows.size() == 1 && numericColumns.size() == 1) {
            return Optional.of(validateOrNone(stat(numericColumns.get(0), "핵심 지표", "단일 숫자 지표는 STAT 표시가 적합합니다."), rows));
        }

        Optional<String> dateColumn = columns.stream()
                .filter(this::isDateLikeColumn)
                .findFirst();
        if (containsAny(normalizedQuestion, TREND_QUESTION_KEYWORDS) && dateColumn.isPresent()) {
            return Optional.of(validateOrNone(axis(
                    AiChartResponse.ChartType.LINE,
                    dateColumn.get(),
                    List.of(numericColumns.get(0)),
                    "기간별 추이",
                    "날짜 기준 숫자 지표 변화는 LINE 차트가 적합합니다."
            ), rows));
        }

        Optional<String> labelColumn = labelColumns.stream()
                .filter(column -> !isDateLikeColumn(column))
                .findFirst();
        if (containsAny(normalizedQuestion, RATIO_QUESTION_KEYWORDS) && labelColumn.isPresent()) {
            return Optional.of(validateOrNone(axis(
                    AiChartResponse.ChartType.DONUT,
                    labelColumn.get(),
                    List.of(numericColumns.get(0)),
                    "구성 비중",
                    "상태, 유형, 비중 분포는 DONUT 차트가 적합합니다."
            ), rows));
        }

        if (containsAny(normalizedQuestion, COMPARISON_QUESTION_KEYWORDS) && labelColumn.isPresent()) {
            return Optional.of(validateOrNone(axis(
                    AiChartResponse.ChartType.BAR,
                    labelColumn.get(),
                    numericColumns.stream().limit(2).toList(),
                    "항목별 비교",
                    "범주별 숫자 지표 비교는 BAR 차트가 적합합니다."
            ), rows));
        }

        return Optional.empty();
    }

    private AiChartResponse validateOrNone(AiChartResponse chart, List<Map<String, Object>> rows) {
        ChartSpecValidationResult validation = chartSpecValidationService.validate(chart, rows);
        if (validation.isValid()) {
            return validation.getChart();
        }
        return AiChartResponse.table("표 형식 조회", validation.getMessage());
    }

    private AiChartResponse stat(String yKey, String title, String reason) {
        AiChartResponse response = new AiChartResponse();
        response.setEnabled(true);
        response.setType(AiChartResponse.ChartType.STAT);
        response.setYKeys(List.of(yKey));
        response.setTitle(title);
        response.setReason(reason);
        return response;
    }

    private AiChartResponse axis(AiChartResponse.ChartType type, String xKey, List<String> yKeys, String title, String reason) {
        AiChartResponse response = new AiChartResponse();
        response.setEnabled(true);
        response.setType(type);
        response.setXKey(xKey);
        response.setYKeys(yKeys);
        response.setTitle(title);
        response.setReason(reason);
        return response;
    }

    private List<String> resolveColumns(List<Map<String, Object>> rows) {
        List<String> columns = new ArrayList<>();
        rows.forEach(row -> row.keySet().forEach(column -> {
            if (!columns.contains(column)) {
                columns.add(column);
            }
        }));
        return columns;
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

    private boolean isIdentifierLikeColumn(String key) {
        String normalizedKey = normalize(key);
        return NON_METRIC_KEYWORDS.stream().anyMatch(normalizedKey::contains);
    }

    private boolean isDateLikeColumn(String key) {
        String normalizedKey = normalize(key);
        return normalizedKey.contains("date")
                || normalizedKey.contains("day")
                || normalizedKey.contains("month")
                || normalizedKey.contains("year")
                || normalizedKey.contains("created_at")
                || normalizedKey.contains("updated_at");
    }

    private boolean isTableQuestion(String question) {
        return containsAny(question, TABLE_QUESTION_KEYWORDS);
    }

    private boolean isAnalyticalQuestion(String question) {
        return containsAny(question, TREND_QUESTION_KEYWORDS)
                || containsAny(question, RATIO_QUESTION_KEYWORDS)
                || containsAny(question, COMPARISON_QUESTION_KEYWORDS);
    }

    private boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
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

package com.ssafy.demo_app.domain.ai.service.interpretation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiQuestionInterpretationService {

    private static final Pattern TARGET_QTY_PATTERN = Pattern.compile("(\\d+)\\s*(개|건|수량)");
    private static final Pattern BOM_ITEM_BEFORE_QTY_PATTERN = Pattern.compile("(.+?)\\s*(\\d+)\\s*개\\s*(만들|생산|제조)");
    private static final Pattern RECENT_DAYS_PATTERN = Pattern.compile("최근\\s*(\\d+)\\s*일");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(v\\d+(?:\\.\\d+)*)", Pattern.CASE_INSENSITIVE);

    public AiQuestionInterpretation interpret(String question) {
        if (question == null || question.isBlank()) {
            return AiQuestionInterpretation.unknown();
        }

        String normalizedQuestion = question.trim();
        String compactQuestion = normalizedQuestion.replaceAll("\\s+", "");
        AiQuestionInterpretation interpretation = new AiQuestionInterpretation();
        interpretation.setDomain(resolveDomain(compactQuestion));
        interpretation.setDataQuestion(!"unknown".equals(interpretation.getDomain()));
        interpretation.setIntent(resolveIntent(compactQuestion, interpretation.getDomain()));
        interpretation.setMetric(resolveMetric(compactQuestion, interpretation.getDomain()));
        interpretation.setDimensions(resolveDimensions(compactQuestion));
        interpretation.setTimeRange(resolveTimeRange(compactQuestion));
        interpretation.setTargetQty(resolveTargetQty(compactQuestion));
        interpretation.setBomVersionPolicy(resolveBomVersionPolicy(compactQuestion));
        interpretation.setBomVersion(resolveBomVersion(compactQuestion));
        applyKeywordSlots(normalizedQuestion, compactQuestion, interpretation);
        interpretation.setSuggestedQuestions(resolveSuggestedQuestions(interpretation));
        interpretation.setSummary(buildSummary(interpretation));
        return interpretation;
    }

    private String resolveDomain(String question) {
        if (containsAny(question, "bom", "BOM", "소요량")
                || (containsAny(question, "필요", "만들", "생산", "제조") && question.contains("버전"))) {
            return "bom";
        }
        if (containsAny(question, "출하", "출고", "피킹", "상차")) {
            return "shipping";
        }
        if (containsAny(question, "입고", "입하", "검수", "적재")) {
            return "inbound";
        }
        if (containsAny(question, "재고", "현재고", "안전재고", "수불", "입출고", "창고", "로케이션")) {
            return "inventory";
        }
        if (containsAny(question, "거래처", "고객사", "공급처", "협력사")) {
            return "partner";
        }
        if (containsAny(question, "생산", "작업지시", "실적", "불량률")) {
            return "production";
        }
        if (containsAny(question, "라우팅", "공장", "라인", "공정")) {
            return "routing";
        }
        if (containsAny(question, "품목", "완제품", "반제품", "원자재")) {
            return "item";
        }
        return "unknown";
    }

    private String resolveIntent(String question, String domain) {
        if ("bom".equals(domain) && containsAny(question, "소요량", "필요", "만들", "생산", "제조")) {
            return "material_requirement";
        }
        if (containsAny(question, "추이", "트렌드", "일별", "월별", "주별")) {
            return "trend";
        }
        if (containsAny(question, "상위", "하위", "top", "TOP", "많은", "높은", "큰", "랭킹")) {
            return "ranking";
        }
        if (containsAny(question, "비교", "대비")) {
            return "comparison";
        }
        if (containsAny(question, "집계", "합계", "건수", "별")) {
            return "aggregate";
        }
        if (containsAny(question, "조회", "보여줘", "알려줘", "목록", "리스트", "찾아")) {
            return "lookup";
        }
        return "lookup";
    }

    private String resolveMetric(String question, String domain) {
        if (containsAny(question, "안전재고미만", "안전재고이하", "안전재고부족", "부족")) {
            return "safety_stock_shortage";
        }
        if (containsAny(question, "현재고", "현재재고", "재고수량")) {
            return "current_stock";
        }
        if (containsAny(question, "수불", "입출고")) {
            return "inventory_transaction";
        }
        if (containsAny(question, "입고수량", "입고량")) {
            return "inbound_qty";
        }
        if (containsAny(question, "출하대기", "출고대기", "대기")) {
            return "shipping_waiting_qty";
        }
        if (question.contains("불량률")) {
            return "defect_rate";
        }
        if (containsAny(question, "생산량", "생산수량")) {
            return "production_qty";
        }
        if ("bom".equals(domain)) {
            return "bom_requirement";
        }
        return "unknown";
    }

    private List<String> resolveDimensions(String question) {
        List<String> dimensions = new ArrayList<>();
        addDimensionIfPresent(dimensions, question, "품목별", "item");
        addDimensionIfPresent(dimensions, question, "거래처별", "partner");
        addDimensionIfPresent(dimensions, question, "고객사별", "partner");
        addDimensionIfPresent(dimensions, question, "공급처별", "partner");
        addDimensionIfPresent(dimensions, question, "창고별", "warehouse");
        addDimensionIfPresent(dimensions, question, "로케이션별", "location");
        addDimensionIfPresent(dimensions, question, "라인별", "line");
        addDimensionIfPresent(dimensions, question, "공정별", "operation");
        addDimensionIfPresent(dimensions, question, "상태별", "status");
        if (containsAny(question, "일별", "월별", "주별")) {
            dimensions.add("date");
        }
        return dimensions;
    }

    private String resolveTimeRange(String question) {
        Matcher recentDaysMatcher = RECENT_DAYS_PATTERN.matcher(question);
        if (recentDaysMatcher.find()) {
            return "recent_" + recentDaysMatcher.group(1) + "_days";
        }
        if (question.contains("오늘")) {
            return "today";
        }
        if (question.contains("어제")) {
            return "yesterday";
        }
        if (question.contains("이번달") || question.contains("이번월")) {
            return "this_month";
        }
        if (question.contains("지난달")) {
            return "last_month";
        }
        return "";
    }

    private Integer resolveTargetQty(String question) {
        Matcher matcher = TARGET_QTY_PATTERN.matcher(question);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    private String resolveBomVersionPolicy(String question) {
        if (containsAny(question, "가장최근", "최신", "latest", "LATEST")) {
            return "LATEST";
        }
        if (VERSION_PATTERN.matcher(question).find()) {
            return "EXPLICIT";
        }
        return "UNSPECIFIED";
    }

    private String resolveBomVersion(String question) {
        Matcher matcher = VERSION_PATTERN.matcher(question);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    private void applyKeywordSlots(
            String originalQuestion,
            String compactQuestion,
            AiQuestionInterpretation interpretation
    ) {
        if ("bom".equals(interpretation.getDomain())) {
            interpretation.setItemKeyword(resolveBomItemKeyword(originalQuestion));
        }
        if (compactQuestion.contains("거래처별") || compactQuestion.contains("고객사별") || compactQuestion.contains("공급처별")) {
            interpretation.setPartnerKeyword("");
        }
        if (compactQuestion.contains("창고")) {
            interpretation.setWarehouseKeyword(resolveTokenBeforeKeyword(originalQuestion, "창고"));
        }
        if (compactQuestion.contains("로케이션")) {
            interpretation.setLocationKeyword(resolveTokenBeforeKeyword(originalQuestion, "로케이션"));
        }
        if (compactQuestion.contains("라인")) {
            interpretation.setLineKeyword(resolveTokenBeforeKeyword(originalQuestion, "라인"));
        }
        if (compactQuestion.contains("공정")) {
            interpretation.setOperationKeyword(resolveTokenBeforeKeyword(originalQuestion, "공정"));
        }
        if (containsAny(compactQuestion, "대기", "완료", "보류", "진행", "취소")) {
            interpretation.setStatusKeyword(resolveStatusKeyword(compactQuestion));
        }
    }

    private String resolveBomItemKeyword(String question) {
        Matcher matcher = BOM_ITEM_BEFORE_QTY_PATTERN.matcher(question);
        if (matcher.find()) {
            return cleanKeyword(matcher.group(1));
        }
        return "";
    }

    private String resolveTokenBeforeKeyword(String question, String keyword) {
        String[] tokens = question.trim().split("\\s+");
        for (int index = 0; index < tokens.length; index += 1) {
            if (tokens[index].contains(keyword) && index > 0) {
                return cleanKeyword(tokens[index - 1]);
            }
        }
        return "";
    }

    private String resolveStatusKeyword(String question) {
        if (question.contains("대기")) {
            return "waiting";
        }
        if (question.contains("완료")) {
            return "completed";
        }
        if (question.contains("보류")) {
            return "hold";
        }
        if (question.contains("진행")) {
            return "running";
        }
        if (question.contains("취소")) {
            return "canceled";
        }
        return "";
    }

    private List<String> resolveSuggestedQuestions(AiQuestionInterpretation interpretation) {
        return switch (interpretation.getDomain()) {
            case "bom" -> List.of(
                    "FG-100 10개 생산 시 최신 BOM 기준 소요량 알려줘",
                    "v2.0 BOM으로 FG-100 10개 만들 때 필요한 품목 보여줘"
            );
            case "inventory" -> List.of(
                    "안전재고 미만 품목을 보여줘",
                    "현재고가 가장 많은 품목 10개를 알려줘"
            );
            case "partner" -> List.of(
                    "거래처별 출하 대기 물량을 집계해줘",
                    "공급처별 입고 수량을 비교해줘"
            );
            case "inbound" -> List.of(
                    "최근 7일 입고 수량 추이를 알려줘",
                    "아직 적재되지 않은 입고 건을 보여줘"
            );
            case "shipping" -> List.of(
                    "출하 대기 건수를 거래처별로 집계해줘",
                    "출하 대기 물량이 큰 거래처 순으로 보여줘"
            );
            case "production" -> List.of(
                    "라인별 불량률을 비교해줘",
                    "이번 달 생산량이 많은 품목 TOP 10을 보여줘"
            );
            default -> AiQuestionInterpretation.unknown().getSuggestedQuestions();
        };
    }

    private String buildSummary(AiQuestionInterpretation interpretation) {
        if (!interpretation.isDataQuestion()) {
            return "데이터 질의로 해석할 수 있는 도메인 단서가 부족합니다.";
        }

        List<String> parts = new ArrayList<>();
        parts.add("도메인=" + interpretation.getDomain());
        parts.add("의도=" + interpretation.getIntent());
        if (!"unknown".equals(interpretation.getMetric())) {
            parts.add("지표=" + interpretation.getMetric());
        }
        if (!interpretation.getDimensions().isEmpty()) {
            parts.add("기준=" + String.join(",", interpretation.getDimensions()));
        }
        if (!interpretation.getTimeRange().isBlank()) {
            parts.add("기간=" + interpretation.getTimeRange());
        }
        if (!interpretation.getItemKeyword().isBlank()) {
            parts.add("품목=" + interpretation.getItemKeyword());
        }
        if (interpretation.getTargetQty() != null) {
            parts.add("수량=" + interpretation.getTargetQty());
        }
        if (!"UNSPECIFIED".equals(interpretation.getBomVersionPolicy())) {
            parts.add("BOM버전=" + interpretation.getBomVersionPolicy().toLowerCase(Locale.ROOT));
        }
        return String.join(" / ", parts);
    }

    private void addDimensionIfPresent(List<String> dimensions, String question, String keyword, String dimension) {
        if (question.contains(keyword) && !dimensions.contains(dimension)) {
            dimensions.add(dimension);
        }
    }

    private boolean containsAny(String question, String... keywords) {
        for (String keyword : keywords) {
            if (question.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String cleanKeyword(String value) {
        return value
                .replace("BOM", "")
                .replace("bom", "")
                .replace("으로", "")
                .replace("로", "")
                .replace("기준", "")
                .replace("최신", "")
                .replace("가장 최근", "")
                .trim();
    }
}

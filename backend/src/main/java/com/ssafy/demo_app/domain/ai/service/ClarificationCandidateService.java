package com.ssafy.demo_app.domain.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ClarificationCandidateService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile(
            "(오늘|어제|이번|지난|최근|\\d+\\s*(일|주|개월|달|년)|월|주|분기|년도|연도)"
    );
    private static final List<String> DEFECT_GROUP_KEYWORDS = List.of(
            "라인별",
            "공정별",
            "품목별",
            "설비별",
            "원인별"
    );
    private static final List<String> COMPARISON_GROUP_KEYWORDS = List.of(
            "라인별",
            "품목별",
            "거래처별",
            "창고별",
            "공정별",
            "일자별",
            "월별"
    );
    private static final List<String> INVENTORY_CONDITION_KEYWORDS = List.of(
            "부족",
            "위험",
            "이하",
            "미만",
            "초과",
            "이상",
            "전체",
            "목록",
            "현황",
            "안전재고"
    );

    public boolean isCandidate(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        String normalizedQuestion = question.replaceAll("\\s+", "");
        return isAmbiguousDefectRate(normalizedQuestion)
                || isAmbiguousTrend(normalizedQuestion)
                || isAmbiguousComparison(normalizedQuestion)
                || isAmbiguousInventory(normalizedQuestion)
                || isAmbiguousShippingOrInboundComparison(normalizedQuestion)
                || isAmbiguousLineStatus(normalizedQuestion)
                || isAmbiguousProductionProblem(normalizedQuestion);
    }

    private boolean isAmbiguousDefectRate(String question) {
        return question.contains("불량률") && containsNone(question, DEFECT_GROUP_KEYWORDS);
    }

    private boolean isAmbiguousTrend(String question) {
        return (question.contains("추이") || question.contains("트렌드"))
                && !PERIOD_PATTERN.matcher(question).find();
    }

    private boolean isAmbiguousComparison(String question) {
        return question.contains("비교") && containsNone(question, COMPARISON_GROUP_KEYWORDS);
    }

    private boolean isAmbiguousInventory(String question) {
        return question.contains("재고") && containsNone(question, INVENTORY_CONDITION_KEYWORDS);
    }

    private boolean isAmbiguousShippingOrInboundComparison(String question) {
        return (question.contains("출고") || question.contains("출하") || question.contains("입고"))
                && question.contains("비교")
                && containsNone(question, COMPARISON_GROUP_KEYWORDS);
    }

    private boolean isAmbiguousLineStatus(String question) {
        return question.contains("라인")
                && (question.contains("상태") || question.contains("현황"))
                && containsNone(question, List.of("라우팅", "생산", "실적", "불량률", "공정별", "라인별"));
    }

    private boolean isAmbiguousProductionProblem(String question) {
        return question.contains("생산")
                && question.contains("문제")
                && containsNone(question, List.of("불량", "지연", "재고", "작업지시", "라인", "공정"));
    }

    private boolean containsNone(String question, List<String> keywords) {
        return keywords.stream().noneMatch(question::contains);
    }
}

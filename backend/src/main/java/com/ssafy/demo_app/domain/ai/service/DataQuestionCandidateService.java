package com.ssafy.demo_app.domain.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataQuestionCandidateService {

    private static final List<String> DOMAIN_KEYWORDS = List.of(
            "입고",
            "출고",
            "출하",
            "재고",
            "현재고",
            "수불",
            "생산",
            "작업지시",
            "작업",
            "실적",
            "불량",
            "불량률",
            "라인",
            "공정",
            "라우팅",
            "설비",
            "bom",
            "BOM",
            "품목",
            "자재",
            "원자재",
            "반제품",
            "완제품",
            "창고",
            "로케이션",
            "거래처",
            "고객사",
            "공급처"
    );

    public boolean isCandidate(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        String normalizedQuestion = question.replaceAll("\\s+", "");
        return DOMAIN_KEYWORDS.stream().anyMatch(normalizedQuestion::contains);
    }
}

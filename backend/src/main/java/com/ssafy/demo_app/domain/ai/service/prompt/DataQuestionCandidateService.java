package com.ssafy.demo_app.domain.ai.service.prompt;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataQuestionCandidateService {

    private static final List<String> STRONG_DOMAIN_KEYWORDS = List.of(
            "입고", "출고", "출하", "재고", "현재고", "수불",
            "생산", "작업지시", "실적", "불량", "불량률",
            "라인", "공정", "라우팅", "bom", "품목", "자재",
            "창고", "로케이션", "거래처", "고객사", "공급처"
    );

    private static final List<String> WEAK_DOMAIN_KEYWORDS = List.of(
            "작업", "운영", "이슈", "위험", "지연", "대기", "문제"
    );

    private static final List<String> DATA_ACTION_KEYWORDS = List.of(
            "조회", "보여줘", "알려줘", "집계", "비교", "추이", "요약", "분석", "순위", "상위", "하위"
    );

    public boolean isCandidate(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        String normalizedQuestion = question.replaceAll("\\s+", "");

        boolean hasStrong = STRONG_DOMAIN_KEYWORDS.stream()
                .anyMatch(normalizedQuestion::contains);

        boolean hasWeak = WEAK_DOMAIN_KEYWORDS.stream()
                .anyMatch(normalizedQuestion::contains);

        boolean hasAction = DATA_ACTION_KEYWORDS.stream()
                .anyMatch(normalizedQuestion::contains);

        return hasStrong || (hasWeak && hasAction);
    }
}

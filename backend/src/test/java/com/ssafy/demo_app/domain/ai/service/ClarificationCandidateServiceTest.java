package com.ssafy.demo_app.domain.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClarificationCandidateServiceTest {

    private final ClarificationCandidateService clarificationCandidateService = new ClarificationCandidateService();

    @Test
    void isCandidate_detectsAmbiguousDefectRate() {
        assertThat(clarificationCandidateService.isCandidate("이번 달 불량률 보여줘")).isTrue();
    }

    @Test
    void isCandidate_skipsSpecificDefectRate() {
        assertThat(clarificationCandidateService.isCandidate("이번 달 라인별 불량률 보여줘")).isFalse();
    }

    @Test
    void isCandidate_detectsTrendWithoutPeriod() {
        assertThat(clarificationCandidateService.isCandidate("입출고 추이 보여줘")).isTrue();
    }

    @Test
    void isCandidate_skipsTrendWithPeriod() {
        assertThat(clarificationCandidateService.isCandidate("최근 7일 입출고 추이 보여줘")).isFalse();
    }

    @Test
    void isCandidate_detectsInventoryWithoutCondition() {
        assertThat(clarificationCandidateService.isCandidate("재고 보여줘")).isTrue();
    }

    @Test
    void isCandidate_detectsShippingComparisonWithoutGroup() {
        assertThat(clarificationCandidateService.isCandidate("출고 비교해줘")).isTrue();
    }

    @Test
    void isCandidate_detectsLineStatusWithoutMetric() {
        assertThat(clarificationCandidateService.isCandidate("라인 상태 알려줘")).isTrue();
    }
}

package com.ssafy.demo_app.domain.ai.service.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataQuestionCandidateServiceTest {

    private final DataQuestionCandidateService dataQuestionCandidateService = new DataQuestionCandidateService();

    @Test
    void isCandidate_detectsDomainQuestions() {
        assertThat(dataQuestionCandidateService.isCandidate("출고 비교해줘")).isTrue();
        assertThat(dataQuestionCandidateService.isCandidate("라인 상태 알려줘")).isTrue();
        assertThat(dataQuestionCandidateService.isCandidate("testparent1 현재 재고 보여줘")).isTrue();
    }

    @Test
    void isCandidate_skipsNonDataQuestions() {
        assertThat(dataQuestionCandidateService.isCandidate("오늘 점심 뭐 먹지?")).isFalse();
    }
}

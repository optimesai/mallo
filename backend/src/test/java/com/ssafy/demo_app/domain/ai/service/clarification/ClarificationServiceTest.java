package com.ssafy.demo_app.domain.ai.service.clarification;

import com.ssafy.demo_app.domain.ai.service.assistant.ClarificationAssistant;
import com.ssafy.demo_app.domain.ai.service.clarification.ClarificationService.ClarificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ClarificationServiceTest {

    @Test
    void evaluate_skipsAssistantWhenQuestionIsNotCandidate() {
        ClarificationCandidateService candidateService = mock(ClarificationCandidateService.class);
        ClarificationAssistant assistant = mock(ClarificationAssistant.class);
        ClarificationService service = new ClarificationService(candidateService, assistant);
        given(candidateService.isCandidate("이번 달 라인별 불량률 보여줘")).willReturn(false);

        ClarificationResult result = service.evaluate("이번 달 라인별 불량률 보여줘", "schema", "2026-06-19 10:00:00");

        assertThat(result.isClarificationRequired()).isFalse();
        verify(assistant, never()).evaluate("schema", "이번 달 라인별 불량률 보여줘", "2026-06-19 10:00:00");
    }

    @Test
    void evaluate_returnsRequiredWhenAssistantReturnsValidJson() {
        ClarificationCandidateService candidateService = mock(ClarificationCandidateService.class);
        ClarificationAssistant assistant = mock(ClarificationAssistant.class);
        ClarificationService service = new ClarificationService(candidateService, assistant);
        given(candidateService.isCandidate("이번 달 불량률 보여줘")).willReturn(true);
        given(assistant.evaluate("schema", "이번 달 불량률 보여줘", "2026-06-19 10:00:00"))
                .willReturn("""
                        {
                          "clarificationRequired": true,
                          "question": "불량률을 라인별, 공정별, 품목별 중 어떤 기준으로 조회할까요?"
                        }
                        """);

        ClarificationResult result = service.evaluate("이번 달 불량률 보여줘", "schema", "2026-06-19 10:00:00");

        assertThat(result.isClarificationRequired()).isTrue();
        assertThat(result.getQuestion()).contains("어떤 기준");
    }

    @Test
    void evaluate_fallsBackWhenAssistantReturnsInvalidJson() {
        ClarificationCandidateService candidateService = mock(ClarificationCandidateService.class);
        ClarificationAssistant assistant = mock(ClarificationAssistant.class);
        ClarificationService service = new ClarificationService(candidateService, assistant);
        given(candidateService.isCandidate("이번 달 불량률 보여줘")).willReturn(true);
        given(assistant.evaluate("schema", "이번 달 불량률 보여줘", "2026-06-19 10:00:00"))
                .willReturn("not-json");

        ClarificationResult result = service.evaluate("이번 달 불량률 보여줘", "schema", "2026-06-19 10:00:00");

        assertThat(result.isClarificationRequired()).isFalse();
    }
}

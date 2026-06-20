package com.ssafy.demo_app.domain.ai.service;

import com.ssafy.demo_app.api.ai.dto.AiQueryResponse;
import com.ssafy.demo_app.domain.ai.entity.AiQueryHistory;
import com.ssafy.demo_app.domain.ai.repository.AiQueryHistoryRepository;
import com.ssafy.demo_app.domain.ai.service.assistant.AnswerGenerator;
import com.ssafy.demo_app.domain.ai.service.assistant.IntentClassifier;
import com.ssafy.demo_app.domain.ai.service.assistant.SqlAssistant;
import com.ssafy.demo_app.domain.ai.service.chart.ChartRecommendationService;
import com.ssafy.demo_app.domain.ai.service.clarification.ClarificationService.ClarificationResult;
import com.ssafy.demo_app.domain.ai.service.clarification.ClarificationService;
import com.ssafy.demo_app.domain.ai.service.prompt.DataQuestionCandidateService;
import com.ssafy.demo_app.domain.ai.service.prompt.FewShotPromptService;
import com.ssafy.demo_app.domain.ai.service.schema.DatabaseSchemaService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlExecutionService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSanitizer;
import com.ssafy.demo_app.domain.ai.service.sql.SqlValidationService;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AiQueryServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiQueryHistoryRepository aiQueryHistoryRepository = mock(AiQueryHistoryRepository.class);
    private final DatabaseSchemaService databaseSchemaService = mock(DatabaseSchemaService.class);
    private final IntentClassifier intentClassifier = mock(IntentClassifier.class);
    private final DataQuestionCandidateService dataQuestionCandidateService = mock(DataQuestionCandidateService.class);
    private final SqlAssistant sqlAssistant = mock(SqlAssistant.class);
    private final FewShotPromptService fewShotPromptService = mock(FewShotPromptService.class);
    private final ClarificationService clarificationService = mock(ClarificationService.class);
    private final SqlSanitizer sqlSanitizer = mock(SqlSanitizer.class);
    private final SqlValidationService sqlValidationService = mock(SqlValidationService.class);
    private final SqlExecutionService sqlExecutionService = mock(SqlExecutionService.class);
    private final AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
    private final ChartRecommendationService chartRecommendationService = mock(ChartRecommendationService.class);

    private final AiQueryServiceImpl aiQueryService = new AiQueryServiceImpl(
            userRepository,
            aiQueryHistoryRepository,
            databaseSchemaService,
            intentClassifier,
            dataQuestionCandidateService,
            sqlAssistant,
            fewShotPromptService,
            clarificationService,
            sqlSanitizer,
            sqlValidationService,
            sqlExecutionService,
            answerGenerator,
            chartRecommendationService
    );

    @Test
    void ask_returnsSchemaLoadFailedWhenSchemaFails() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willThrow(new IllegalStateException("metadata failed"));

        AiQueryResponse response = aiQueryService.ask(1, "이번 달 불량률 보여줘");

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.SCHEMA_LOAD_FAILED);
        assertThat(response.getAnswer()).isEqualTo("데이터 스키마 정보를 불러오는 중 오류가 발생했습니다.");
        assertThat(response.getRows()).isEqualTo(List.of());
        verify(intentClassifier, never()).classify(any(), any(), any());
        verify(sqlAssistant, never()).generateSql(any(), any(), any(), any());
    }

    @Test
    void ask_returnsClarificationWhenQuestionIsAmbiguous() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(intentClassifier.classify(any(), any(), any())).willReturn("YES");
        given(dataQuestionCandidateService.isCandidate("이번 달 불량률 보여줘")).willReturn(true);
        given(fewShotPromptService.getFewShotExamples()).willReturn("few shot");
        given(clarificationService.evaluate(any(), any(), any()))
                .willReturn(ClarificationResult.required("불량률을 라인별, 공정별, 품목별 중 어떤 기준으로 조회할까요?"));

        AiQueryResponse response = aiQueryService.ask(1, "이번 달 불량률 보여줘");

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED);
        assertThat(response.getClarificationRequired()).isTrue();
        assertThat(response.getAnswer()).isEqualTo("질문을 조금 더 구체화해주세요.");
        assertThat(response.getClarificationQuestion()).contains("어떤 기준");
        verify(sqlAssistant, never()).generateSql(any(), any(), any(), any());
        verify(sqlExecutionService, never()).execute(any());
    }

    private void givenDefaultUser() {
        User user = new User();
        user.setUserId(1);
        user.setEmployeeNo("E001");
        user.setUserName("테스트 사용자");
        user.setDepartment("생산");
        user.setPassword("password");
        user.setRole(User.Role.WORKER);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
    }
}

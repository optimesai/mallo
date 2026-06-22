package com.ssafy.demo_app.domain.ai.service;

import com.ssafy.demo_app.api.ai.dto.AiQueryRequest;
import com.ssafy.demo_app.api.ai.dto.AiQueryResponse;
import com.ssafy.demo_app.domain.ai.entity.AiQueryHistory;
import com.ssafy.demo_app.domain.ai.repository.AiQueryHistoryRepository;
import com.ssafy.demo_app.domain.ai.service.assistant.AnswerGenerator;
import com.ssafy.demo_app.domain.ai.service.assistant.SqlAssistant;
import com.ssafy.demo_app.domain.ai.service.chart.ChartRecommendationService;
import com.ssafy.demo_app.domain.ai.service.classification.AiIntentResult;
import com.ssafy.demo_app.domain.ai.service.classification.IntentClassificationService;
import com.ssafy.demo_app.domain.ai.service.clarification.ClarificationService.ClarificationResult;
import com.ssafy.demo_app.domain.ai.service.clarification.ClarificationService;
import com.ssafy.demo_app.domain.ai.service.interpretation.AiQuestionInterpretationService;
import com.ssafy.demo_app.domain.ai.service.prompt.DataQuestionCandidateService;
import com.ssafy.demo_app.domain.ai.service.prompt.FewShotPromptService;
import com.ssafy.demo_app.domain.ai.service.rule.BusinessRulePromptService;
import com.ssafy.demo_app.domain.ai.service.schema.DatabaseSchemaService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlExecutionService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlReviewService.SqlReviewResult;
import com.ssafy.demo_app.domain.ai.service.sql.SqlReviewService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSanitizer;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSemanticValidationService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSemanticValidationService.SqlSemanticValidationResult;
import com.ssafy.demo_app.domain.ai.service.sql.SqlValidationService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlValidationService.SqlValidationResult;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AiQueryServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiQueryHistoryRepository aiQueryHistoryRepository = mock(AiQueryHistoryRepository.class);
    private final DatabaseSchemaService databaseSchemaService = mock(DatabaseSchemaService.class);
    private final BusinessRulePromptService businessRulePromptService = mock(BusinessRulePromptService.class);
    private final IntentClassificationService intentClassificationService = mock(IntentClassificationService.class);
    private final DataQuestionCandidateService dataQuestionCandidateService = mock(DataQuestionCandidateService.class);
    private final AiQuestionInterpretationService aiQuestionInterpretationService = new AiQuestionInterpretationService();
    private final SqlAssistant sqlAssistant = mock(SqlAssistant.class);
    private final FewShotPromptService fewShotPromptService = mock(FewShotPromptService.class);
    private final ClarificationService clarificationService = mock(ClarificationService.class);
    private final SqlSanitizer sqlSanitizer = mock(SqlSanitizer.class);
    private final SqlReviewService sqlReviewService = mock(SqlReviewService.class);
    private final SqlValidationService sqlValidationService = mock(SqlValidationService.class);
    private final SqlSemanticValidationService sqlSemanticValidationService = mock(SqlSemanticValidationService.class);
    private final SqlExecutionService sqlExecutionService = mock(SqlExecutionService.class);
    private final AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
    private final ChartRecommendationService chartRecommendationService = mock(ChartRecommendationService.class);

    private final AiQueryServiceImpl aiQueryService = new AiQueryServiceImpl(
            userRepository,
            aiQueryHistoryRepository,
            databaseSchemaService,
            businessRulePromptService,
            intentClassificationService,
            dataQuestionCandidateService,
            aiQuestionInterpretationService,
            sqlAssistant,
            fewShotPromptService,
            clarificationService,
            sqlSanitizer,
            sqlReviewService,
            sqlValidationService,
            sqlSemanticValidationService,
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

        AiQueryResponse response = aiQueryService.ask(1, request("이번 달 불량률 보여줘"));

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.SCHEMA_LOAD_FAILED);
        assertThat(response.getAnswer()).isEqualTo("데이터 스키마 정보를 불러오는 중 오류가 발생했습니다.");
        assertThat(response.getRows()).isEqualTo(List.of());
        verify(intentClassificationService, never()).classify(any(), any(), any(), any());
        verify(sqlAssistant, never()).generateSql(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ask_returnsClarificationWhenQuestionIsAmbiguous() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(businessRulePromptService.getBusinessRules()).willReturn("business rules");
        given(intentClassificationService.classify(any(), any(), any(), any())).willReturn(AiIntentResult.dataQuestion());
        given(dataQuestionCandidateService.isCandidate("이번 달 불량률 보여줘")).willReturn(true);
        given(fewShotPromptService.getFewShotExamples()).willReturn("few shot");
        given(clarificationService.evaluate(any(), any(), any()))
                .willReturn(ClarificationResult.required("불량률을 라인별, 공정별, 품목별 중 어떤 기준으로 조회할까요?"));

        AiQueryResponse response = aiQueryService.ask(1, request("이번 달 불량률 보여줘"));

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED);
        assertThat(response.getClarificationRequired()).isTrue();
        assertThat(response.getAnswer()).isEqualTo("질문을 조금 더 구체화해주세요.");
        assertThat(response.getClarificationQuestion()).contains("어떤 기준");
        verify(sqlAssistant, never()).generateSql(any(), any(), any(), any(), any(), any(), any());
        verify(sqlExecutionService, never()).execute(any());
    }

    @Test
    void ask_retriesSqlGenerationWhenSemanticValidationFails() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(businessRulePromptService.getBusinessRules()).willReturn("business rules");
        given(intentClassificationService.classify(any(), any(), any(), any())).willReturn(AiIntentResult.dataQuestion());
        given(dataQuestionCandidateService.isCandidate("안전재고 미만 품목을 보여줘")).willReturn(true);
        given(fewShotPromptService.getFewShotExamples()).willReturn("few shot");
        given(clarificationService.evaluate(any(), any(), any())).willReturn(ClarificationResult.notRequired());
        given(sqlAssistant.generateSql(any(), any(), any(), any(), any(), any(), any()))
                .willReturn("SELECT item_id FROM item_master", "SELECT im.item_id, im.safety_stock, SUM(ci.current_qty) AS current_qty FROM item_master im LEFT JOIN current_inventory ci ON im.item_id = ci.item_id GROUP BY im.item_id, im.safety_stock HAVING SUM(ci.current_qty) < im.safety_stock");
        given(sqlSanitizer.sanitize(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(sqlValidationService.validate(any()))
                .willReturn(SqlValidationResult.valid("SELECT item_id FROM item_master"))
                .willReturn(SqlValidationResult.valid("SELECT im.item_id, im.safety_stock, SUM(ci.current_qty) AS current_qty FROM item_master im LEFT JOIN current_inventory ci ON im.item_id = ci.item_id GROUP BY im.item_id, im.safety_stock HAVING SUM(ci.current_qty) < im.safety_stock"));
        given(sqlSemanticValidationService.validate(any(), any(), any()))
                .willReturn(SqlSemanticValidationResult.invalid("안전재고 비교가 필요합니다."))
                .willReturn(SqlSemanticValidationResult.valid());
        given(sqlExecutionService.execute(any())).willReturn(List.of());
        given(answerGenerator.generateAnswer(any(), any(), any(), any(), any())).willReturn("조건에 맞는 데이터가 없습니다.");

        AiQueryResponse response = aiQueryService.ask(1, request("안전재고 미만 품목을 보여줘"));

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.SUCCESS);
        verify(sqlAssistant, times(2)).generateSql(any(), any(), any(), any(), any(), any(), any());
        verify(sqlExecutionService).execute(any());
    }

    @Test
    void ask_usesInterpretationWhenClassifierMissesDataQuestion() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(businessRulePromptService.getBusinessRules()).willReturn("business rules");
        given(intentClassificationService.classify(any(), any(), any(), any())).willReturn(AiIntentResult.notDataQuestion());
        given(dataQuestionCandidateService.isCandidate("최근 7일 입고 수량 추이를 알려줘")).willReturn(false);
        given(fewShotPromptService.getFewShotExamples(any(), any())).willReturn("few shot");
        given(clarificationService.evaluate(any(), any(), any())).willReturn(ClarificationResult.notRequired());
        given(sqlAssistant.generateSql(any(), any(), any(), any(), any(), any(), any()))
                .willReturn("SELECT inbound_date, SUM(inbound_qty) AS total_inbound_qty FROM inbound_receipt GROUP BY inbound_date");
        given(sqlSanitizer.sanitize(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(sqlValidationService.validate(any()))
                .willReturn(SqlValidationResult.valid("SELECT inbound_date, SUM(inbound_qty) AS total_inbound_qty FROM inbound_receipt GROUP BY inbound_date"));
        given(sqlSemanticValidationService.validate(any(), any(), any()))
                .willReturn(SqlSemanticValidationResult.valid());
        given(sqlExecutionService.execute(any())).willReturn(List.of());
        given(answerGenerator.generateAnswer(any(), any(), any(), any(), any())).willReturn("최근 7일 입고 수량 추이입니다.");

        AiQueryResponse response = aiQueryService.ask(1, request("최근 7일 입고 수량 추이를 알려줘"));

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.SUCCESS);
        assertThat(response.getInterpretedDomain()).isEqualTo("inbound");
        assertThat(response.getInterpretedIntent()).isEqualTo("trend");
        assertThat(response.getInterpretationSummary()).contains("도메인=inbound");
        assertThat(response.getSuggestedQuestions()).isNotEmpty();
        verify(sqlAssistant).generateSql(any(), any(), any(), any(), any(), any(), any());
        verify(sqlExecutionService).execute(any());
    }

    @Test
    void ask_regeneratesSqlWhenSqlReviewFails() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(businessRulePromptService.getBusinessRules()).willReturn("business rules");
        given(intentClassificationService.classify(any(), any(), any(), any())).willReturn(AiIntentResult.dataQuestion());
        given(dataQuestionCandidateService.isCandidate(any())).willReturn(true);
        given(fewShotPromptService.getFewShotExamples(any(), any())).willReturn("few shot");
        given(clarificationService.evaluate(any(), any(), any())).willReturn(ClarificationResult.notRequired());
        given(sqlAssistant.generateSql(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(
                        "SELECT child.item_code FROM bom_structure bs JOIN item_master parent ON bs.parent_item_id = parent.item_id JOIN item_master child ON bs.child_item_id = child.item_id WHERE parent.item_code = 'testparent'",
                        "SELECT child.item_code FROM bom_structure bs JOIN item_master parent ON bs.parent_item_id = parent.item_id JOIN item_master child ON bs.child_item_id = child.item_id WHERE (LOWER(parent.item_code) = LOWER('testparent') OR LOWER(parent.item_name) = LOWER('testparent'))"
                );
        given(sqlSanitizer.sanitize(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(sqlReviewService.review(any(), any(), any(), any(), any()))
                .willReturn(SqlReviewResult.invalid(
                        "품목 필터가 item_code만 사용되었습니다.",
                        "item_code, item_name, item_id를 모두 고려하세요."
                ))
                .willReturn(SqlReviewResult.valid("해석 슬롯을 반영했습니다."));
        given(sqlValidationService.validate(any()))
                .willReturn(SqlValidationResult.valid("SELECT child.item_code FROM bom_structure bs JOIN item_master parent ON bs.parent_item_id = parent.item_id JOIN item_master child ON bs.child_item_id = child.item_id WHERE (LOWER(parent.item_code) = LOWER('testparent') OR LOWER(parent.item_name) = LOWER('testparent'))"));
        given(sqlSemanticValidationService.validate(any(), any(), any()))
                .willReturn(SqlSemanticValidationResult.valid());
        given(sqlExecutionService.execute(any())).willReturn(List.of());
        given(answerGenerator.generateAnswer(any(), any(), any(), any(), any())).willReturn("조회 결과입니다.");

        AiQueryResponse response = aiQueryService.ask(
                1,
                request("testparent 100개 생산하려면 필요한 자재 수량을 알려줘. 가장 최신 버전 BOM 기준으로")
        );

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.SUCCESS);
        assertThat(response.getInterpretedDomain()).isEqualTo("bom");
        verify(sqlAssistant, times(2)).generateSql(any(), any(), any(), any(), any(), any(), any());
        verify(sqlReviewService, times(2)).review(any(), any(), any(), any(), any());
        verify(sqlExecutionService).execute(any());
    }

    @Test
    void ask_returnsClarificationGuideWhenSemanticValidationStillFailsAfterRetry() {
        givenDefaultUser();
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(businessRulePromptService.getBusinessRules()).willReturn("business rules");
        given(intentClassificationService.classify(any(), any(), any(), any())).willReturn(AiIntentResult.dataQuestion());
        given(dataQuestionCandidateService.isCandidate("재고 문제 보여줘")).willReturn(true);
        given(fewShotPromptService.getFewShotExamples()).willReturn("few shot");
        given(clarificationService.evaluate(any(), any(), any())).willReturn(ClarificationResult.notRequired());
        given(sqlAssistant.generateSql(any(), any(), any(), any(), any(), any(), any()))
                .willReturn("SELECT item_id FROM item_master", "SELECT item_id FROM item_master");
        given(sqlSanitizer.sanitize(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(sqlValidationService.validate(any()))
                .willReturn(SqlValidationResult.valid("SELECT item_id FROM item_master"))
                .willReturn(SqlValidationResult.valid("SELECT item_id FROM item_master"));
        given(sqlSemanticValidationService.validate(any(), any(), any()))
                .willReturn(SqlSemanticValidationResult.invalid("재고 도메인 질의에는 재고 또는 수불 테이블 사용이 필요합니다."))
                .willReturn(SqlSemanticValidationResult.invalid("재고 도메인 질의에는 재고 또는 수불 테이블 사용이 필요합니다."));

        AiQueryResponse response = aiQueryService.ask(1, request("재고 문제 보여줘"));

        assertThat(response.getExecutionStatus()).isEqualTo(AiQueryHistory.ExecutionStatus.SEMANTIC_VALIDATION_FAILED);
        assertThat(response.getAnswer()).contains("질문을 조금 더 구체화해주세요");
        assertThat(response.getAnswer()).contains("조회 기간");
        assertThat(response.getAnswer()).contains("집계 기준");
        verify(sqlExecutionService, never()).execute(any());
    }

    @Test
    void ask_mergesClarificationAnswerWithParentQuestion() {
        givenDefaultUser();
        AiQueryHistory parentHistory = new AiQueryHistory();
        parentHistory.setQueryId(10);
        parentHistory.setNaturalQuestion("이번 달 불량률 보여줘");
        parentHistory.setEffectiveQuestion("이번 달 불량률 보여줘");
        parentHistory.setNaturalAnswer("불량률을 라인별, 공정별, 품목별 중 어떤 기준으로 조회할까요?");
        parentHistory.setConversationId("conversation-1");
        parentHistory.setExecutionStatus(AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED);

        given(aiQueryHistoryRepository.findByQueryIdAndWorker(eq(10), any(User.class)))
                .willReturn(Optional.of(parentHistory));
        given(aiQueryHistoryRepository.save(any(AiQueryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(databaseSchemaService.getSchemaDescription()).willReturn("schema");
        given(businessRulePromptService.getBusinessRules()).willReturn("business rules");
        AiIntentResult intentResult = AiIntentResult.dataQuestion();
        intentResult.setNeedsClarification(true);
        intentResult.setClarificationQuestion("조회 기간을 지정할까요?");
        given(intentClassificationService.classify(any(), any(), any(), any())).willReturn(intentResult);

        AiQueryResponse response = aiQueryService.ask(1, clarificationRequest("라인별", 10));

        ArgumentCaptor<String> questionCaptor = ArgumentCaptor.forClass(String.class);
        verify(intentClassificationService).classify(questionCaptor.capture(), any(), any(), any());
        assertThat(questionCaptor.getValue()).contains("원래 질문: 이번 달 불량률 보여줘");
        assertThat(questionCaptor.getValue()).contains("사용자 추가 답변: 라인별");
        assertThat(response.getQuestion()).isEqualTo("라인별");
        assertThat(response.getEffectiveQuestion()).contains("사용자 추가 답변: 라인별");
        assertThat(response.getClarificationOfQueryId()).isEqualTo(10);
        verify(sqlAssistant, never()).generateSql(any(), any(), any(), any(), any(), any(), any());
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
        given(sqlReviewService.review(any(), any(), any(), any(), any()))
                .willReturn(SqlReviewResult.valid("success"));
    }

    private AiQueryRequest request(String question) {
        AiQueryRequest request = new AiQueryRequest();
        request.setQuestion(question);
        request.setConversationId("conversation-1");
        return request;
    }

    private AiQueryRequest clarificationRequest(String question, Integer parentQueryId) {
        AiQueryRequest request = request(question);
        request.setClarificationOfQueryId(parentQueryId);
        return request;
    }
}

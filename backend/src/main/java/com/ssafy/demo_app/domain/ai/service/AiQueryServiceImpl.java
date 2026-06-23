package com.ssafy.demo_app.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
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
import com.ssafy.demo_app.domain.ai.service.interpretation.AiQuestionInterpretation;
import com.ssafy.demo_app.domain.ai.service.interpretation.AiQuestionInterpretationService;
import com.ssafy.demo_app.domain.ai.service.prompt.DataQuestionCandidateService;
import com.ssafy.demo_app.domain.ai.service.prompt.FewShotPromptService;
import com.ssafy.demo_app.domain.ai.service.rule.BusinessRulePromptService;
import com.ssafy.demo_app.domain.ai.service.schema.DatabaseSchemaService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlExecutionService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlReviewService.SqlReviewResult;
import com.ssafy.demo_app.domain.ai.service.sql.SqlReviewService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSanitizer;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSemanticValidationService.SqlSemanticValidationResult;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSemanticValidationService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlValidationService.SqlValidationResult;
import com.ssafy.demo_app.domain.ai.service.sql.SqlValidationService;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiQueryServiceImpl implements AiQueryService {

    private static final DateTimeFormatter CURRENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String NOT_DATA_QUESTION_ANSWER = "데이터 조회와 관련 없는 질문입니다.";
    private static final String SCHEMA_LOAD_FAILED_ANSWER = "데이터 스키마 정보를 불러오는 중 오류가 발생했습니다.";
    private static final String CLARIFICATION_REQUIRED_ANSWER = "질문을 조금 더 구체화해주세요.";
    private static final String SQL_GENERATION_FAILED_ANSWER = "SQL 생성 중 오류가 발생했습니다.";
    private static final String BLOCKED_SQL_ANSWER = "보안 정책상 실행할 수 없는 요청입니다.";
    private static final String SEMANTIC_VALIDATION_FAILED_ANSWER = "질문을 조금 더 구체화해주세요. 예: 조회 기간, 집계 기준(품목별/거래처별/라인별), 상태값, 대상 품목 또는 거래처를 함께 입력하면 더 정확하게 조회할 수 있습니다.";
    private static final String SQL_EXECUTION_FAILED_ANSWER = "쿼리 실행 중 오류가 발생했습니다.";
    private static final String ANSWER_GENERATION_FAILED_ANSWER = "답변 생성 중 오류가 발생했습니다.";
    private static final String NEW_QUESTION_MARKERS = "이번에는|이번엔|이번 질문|새로|다른|말고|대신";
    private static final String REQUEST_MARKERS = "알려줘|보여줘|조회|집계|분석|확인|찾아|추천|뽑아|나열|리스트|어떤|무엇|뭐|얼마나|몇";
    private static final String CONDITION_MARKERS = "기준|만|별|상위|하위|최근|지난|이번|오늘|어제|내일|개월|일|주|월|년|창고|공장|라인|거래처|품목|완제품|반제품|원자재|안전재고|이상|이하|미만|초과|부터|까지";

    private final UserRepository userRepository;
    private final AiQueryHistoryRepository aiQueryHistoryRepository;
    private final DatabaseSchemaService databaseSchemaService;
    private final BusinessRulePromptService businessRulePromptService;
    private final IntentClassificationService intentClassificationService;
    private final DataQuestionCandidateService dataQuestionCandidateService;
    private final AiQuestionInterpretationService aiQuestionInterpretationService;
    private final SqlAssistant sqlAssistant;
    private final FewShotPromptService fewShotPromptService;
    private final ClarificationService clarificationService;
    private final SqlSanitizer sqlSanitizer;
    private final SqlReviewService sqlReviewService;
    private final SqlValidationService sqlValidationService;
    private final SqlSemanticValidationService sqlSemanticValidationService;
    private final SqlExecutionService sqlExecutionService;
    private final AnswerGenerator answerGenerator;
    private final ChartRecommendationService chartRecommendationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${langchain4j.open-ai.chat-model.model-name:unknown}")
    private String modelName;

    @Override
    @Transactional
    public AiQueryResponse ask(Integer userId, AiQueryRequest request) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionContext questionContext = resolveQuestionContext(worker, request);
        String effectiveQuestion = questionContext.getEffectiveQuestion();
        AiQuestionInterpretation interpretation = aiQuestionInterpretationService.interpret(effectiveQuestion);
        long startedAt = System.currentTimeMillis();
        String currentTime = LocalDateTime.now().format(CURRENT_TIME_FORMATTER);
        String schema;
        try {
            schema = databaseSchemaService.getSchemaDescription();
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    SCHEMA_LOAD_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SCHEMA_LOAD_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of(), interpretation);
        }

        String businessRules = businessRulePromptService.getBusinessRules();
        AiIntentResult intentResult;
        try {
            intentResult = intentClassificationService.classify(effectiveQuestion, schema, businessRules, currentTime);
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    SQL_GENERATION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of(), interpretation);
        }

        applyInterpretation(intentResult, interpretation);
        boolean dataQuestionCandidate = dataQuestionCandidateService.isCandidate(effectiveQuestion);
        if (!intentResult.isDataQuestion() && !dataQuestionCandidate && !interpretation.isDataQuestion()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    NOT_DATA_QUESTION_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.NOT_DATA_QUESTION,
                    null
            );
            return toResponse(history, List.of(), interpretation);
        }
        if (!intentResult.isDataQuestion()) {
            intentResult.setDataQuestion(true);
            intentResult.setReason(resolveDataQuestionReason(interpretation));
        }
        if (intentResult.isNeedsClarification() && !intentResult.getClarificationQuestion().isBlank()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    intentResult.getClarificationQuestion(),
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED,
                    null
            );
            return toResponse(history, List.of(), interpretation);
        }

        String fewShotExamples;
        try {
            fewShotExamples = fewShotPromptService.getFewShotExamples(
                    intentResult.getDomain(),
                    intentResult.getIntent()
            );
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    SQL_GENERATION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of(), interpretation);
        }

        ClarificationResult clarification = clarificationService.evaluate(effectiveQuestion, schema, currentTime);
        if (clarification.isClarificationRequired()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    clarification.getQuestion(),
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED,
                    null
            );
            return toResponse(history, List.of(), interpretation);
        }

        String classificationResult = toJson(intentResult, interpretation);
        ValidatedSql validatedSql;
        try {
            validatedSql = generateValidatedSql(
                    effectiveQuestion,
                    schema,
                    businessRules,
                    classificationResult,
                    intentResult,
                    fewShotExamples,
                    currentTime
            );
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    null,
                    SQL_GENERATION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of(), interpretation);
        }

        if (!validatedSql.isValid()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    validatedSql.getSql(),
                    resolveValidationFailureAnswer(validatedSql),
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    validatedSql.getStatus(),
                    validatedSql.getMessage()
            );
            return toResponse(history, List.of(), interpretation);
        }

        List<Map<String, Object>> rows;
        try {
            rows = sqlExecutionService.execute(validatedSql.getNormalizedSql());
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    validatedSql.getNormalizedSql(),
                    SQL_EXECUTION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_EXECUTION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of(), interpretation);
        }

        String resultJson = toJson(rows);
        String answer;
        try {
            answer = answerGenerator.generateAnswer(
                    effectiveQuestion,
                    classificationResult,
                    businessRules,
                    validatedSql.getNormalizedSql(),
                    resultJson
            );
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    questionContext,
                    validatedSql.getNormalizedSql(),
                    ANSWER_GENERATION_FAILED_ANSWER,
                    resultJson,
                    null,
                    rows.size(),
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.ANSWER_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, rows, interpretation);
        }

        AiChartResponse chart = chartRecommendationService.recommend(effectiveQuestion, intentResult, rows);
        String chartSpecJson = toJson(chart);
        AiQueryHistory history = saveHistory(
                worker,
                questionContext,
                validatedSql.getNormalizedSql(),
                answer,
                resultJson,
                chartSpecJson,
                rows.size(),
                elapsedTime(startedAt),
                AiQueryHistory.ExecutionStatus.SUCCESS,
                null
        );
        return toResponse(history, rows, interpretation);
    }

    private QuestionContext resolveQuestionContext(User worker, AiQueryRequest request) {
        String originalQuestion = normalizeQuestion(request.getQuestion());
        String conversationId = resolveConversationId(request.getConversationId());
        AiQueryHistory pendingHistory = resolvePendingClarification(worker, conversationId, request.getClarificationOfQueryId());

        if (pendingHistory == null) {
            return QuestionContext.of(originalQuestion, originalQuestion, conversationId, null);
        }

        if (isExplicitNewQuestion(originalQuestion)) {
            cancelPendingClarification(pendingHistory);
            return QuestionContext.of(originalQuestion, originalQuestion, conversationId, null);
        }

        return QuestionContext.of(
                originalQuestion,
                buildEffectiveQuestion(pendingHistory, originalQuestion),
                conversationId,
                pendingHistory.getQueryId()
        );
    }

    private AiQueryHistory resolvePendingClarification(User worker, String conversationId, Integer requestedParentQueryId) {
        AiQueryHistory latestPendingHistory = aiQueryHistoryRepository
                .findFirstByWorkerAndConversationIdAndExecutionStatusOrderByCreatedAtDesc(
                        worker,
                        conversationId,
                        AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED
                )
                .orElse(null);

        if (latestPendingHistory != null) {
            return latestPendingHistory;
        }
        if (requestedParentQueryId == null) {
            return null;
        }

        AiQueryHistory requestedParentHistory = aiQueryHistoryRepository.findByQueryIdAndWorker(requestedParentQueryId, worker)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        if (requestedParentHistory.getExecutionStatus() != AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (requestedParentHistory.getConversationId() != null
                && !requestedParentHistory.getConversationId().isBlank()
                && !requestedParentHistory.getConversationId().equals(conversationId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return requestedParentHistory;
    }

    private boolean isExplicitNewQuestion(String question) {
        String normalizedQuestion = question.replaceAll("\\s+", "");
        if (isConditionOnlyFollowUp(question)) {
            return false;
        }
        return normalizedQuestion.matches(".*(" + NEW_QUESTION_MARKERS + ").*")
                && normalizedQuestion.matches(".*(" + REQUEST_MARKERS + ").*");
    }

    private boolean isConditionOnlyFollowUp(String question) {
        String normalizedQuestion = question.replaceAll("\\s+", "");
        boolean shortInput = normalizedQuestion.length() <= 30;
        boolean hasConditionMarker = normalizedQuestion.matches(".*(" + CONDITION_MARKERS + ").*");
        boolean hasRequestMarker = normalizedQuestion.matches(".*(" + REQUEST_MARKERS + ").*");
        boolean hasNewQuestionMarker = normalizedQuestion.matches(".*(" + NEW_QUESTION_MARKERS + ").*");
        return shortInput && hasConditionMarker && (!hasRequestMarker || !hasNewQuestionMarker);
    }

    private void cancelPendingClarification(AiQueryHistory pendingHistory) {
        pendingHistory.setExecutionStatus(AiQueryHistory.ExecutionStatus.CANCELLED);
        pendingHistory.setErrorLog("사용자가 pending clarification 상태에서 새 질문을 입력하여 취소했습니다.");
    }

    private String normalizeQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return question.trim();
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId.trim();
    }

    private String buildEffectiveQuestion(AiQueryHistory parentHistory, String followUpAnswer) {
        String clarificationQuestion = parentHistory.getNaturalAnswer();
        return """
                원래 질문: %s
                AI 추가 확인 질문: %s
                사용자 추가 답변: %s
                위 추가 답변을 원래 질문의 보완 정보로 반영하여 하나의 데이터 조회 질문으로 처리한다.
                """.formatted(
                parentHistory.getEffectiveQuestion() == null || parentHistory.getEffectiveQuestion().isBlank()
                        ? parentHistory.getNaturalQuestion()
                        : parentHistory.getEffectiveQuestion(),
                clarificationQuestion == null ? "" : clarificationQuestion,
                followUpAnswer
        ).trim();
    }

    private void applyInterpretation(AiIntentResult intentResult, AiQuestionInterpretation interpretation) {
        if (intentResult == null || interpretation == null || !interpretation.isDataQuestion()) {
            return;
        }
        if (!intentResult.isDataQuestion()) {
            intentResult.setDataQuestion(true);
        }
        if (isBlankOrUnknown(intentResult.getDomain())) {
            intentResult.setDomain(interpretation.getDomain());
        }
        if (isBlankOrUnknown(intentResult.getIntent())) {
            intentResult.setIntent(interpretation.getIntent());
        }
        if (isBlankOrUnknown(intentResult.getMetric())) {
            intentResult.setMetric(interpretation.getMetric());
        }
        if ((intentResult.getDimensions() == null || intentResult.getDimensions().isEmpty())
                && !interpretation.getDimensions().isEmpty()) {
            intentResult.setDimensions(interpretation.getDimensions());
        }
        if (isBlankOrUnknown(intentResult.getChartHint())) {
            intentResult.setChartHint(resolveChartHint(interpretation));
        }
    }

    private String resolveChartHint(AiQuestionInterpretation interpretation) {
        if ("trend".equals(interpretation.getIntent())) {
            return "LINE";
        }
        if ("aggregate".equals(interpretation.getIntent())
                || "ranking".equals(interpretation.getIntent())
                || "comparison".equals(interpretation.getIntent())) {
            return "BAR";
        }
        return "TABLE";
    }

    private String resolveDataQuestionReason(AiQuestionInterpretation interpretation) {
        if (interpretation != null && interpretation.isDataQuestion()) {
            return "질의 해석 계층에서 %s 도메인 데이터 질의로 판단했습니다.".formatted(interpretation.getDomain());
        }
        return "도메인 키워드 기반 데이터 질의 후보로 판단했습니다.";
    }

    private boolean isBlankOrUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value);
    }

    private ValidatedSql generateValidatedSql(
            String question,
            String schema,
            String businessRules,
            String classificationResult,
            AiIntentResult intentResult,
            String fewShotExamples,
            String currentTime
    ) {
        String sql = generateReviewedSql(
                question,
                schema,
                businessRules,
                classificationResult,
                fewShotExamples,
                currentTime,
                ""
        );
        ValidatedSql firstResult = validateSql(question, intentResult, sql);
        if (firstResult.isValid()) {
            return firstResult;
        }

        String retriedSql = generateReviewedSql(
                question,
                schema,
                businessRules,
                classificationResult,
                fewShotExamples,
                currentTime,
                firstResult.getMessage()
        );
        ValidatedSql retryResult = validateSql(question, intentResult, retriedSql);
        if (retryResult.isValid()) {
            return retryResult;
        }
        return retryResult;
    }

    private String generateReviewedSql(
            String question,
            String schema,
            String businessRules,
            String classificationResult,
            String fewShotExamples,
            String currentTime,
            String retryReason
    ) {
        String effectiveRetryReason = retryReason;
        String sql = generateSql(
                question,
                schema,
                businessRules,
                classificationResult,
                fewShotExamples,
                currentTime,
                effectiveRetryReason
        );
        SqlReviewResult reviewResult = sqlReviewService.review(
                question,
                schema,
                businessRules,
                classificationResult,
                sql
        );
        if (reviewResult.isValid()) {
            return sql;
        }

        String reviewRetryReason = "SQL 검토 실패: %s. 재생성 지시: %s".formatted(
                reviewResult.getReason(),
                reviewResult.getRetryInstruction()
        );
        effectiveRetryReason = joinRetryReasons(effectiveRetryReason, reviewRetryReason);
        sql = generateSql(
                question,
                schema,
                businessRules,
                classificationResult,
                fewShotExamples,
                currentTime,
                effectiveRetryReason
        );
        reviewResult = sqlReviewService.review(
                question,
                schema,
                businessRules,
                classificationResult,
                sql
        );
        return sql;
    }

    private String joinRetryReasons(String originalRetryReason, String reviewRetryReason) {
        if (originalRetryReason == null || originalRetryReason.isBlank()) {
            return reviewRetryReason;
        }
        return originalRetryReason + "\n" + reviewRetryReason;
    }

    private ValidatedSql validateSql(String question, AiIntentResult intentResult, String sql) {
        SqlValidationResult validation = sqlValidationService.validate(sql);
        if (!validation.isValid()) {
            return ValidatedSql.invalid(
                    sql,
                    validation.getMessage(),
                    AiQueryHistory.ExecutionStatus.BLOCKED_UNSAFE_SQL
            );
        }

        SqlSemanticValidationResult semanticValidation = sqlSemanticValidationService.validate(
                question,
                intentResult,
                validation.getNormalizedSql()
        );
        if (!semanticValidation.isValid()) {
            return ValidatedSql.invalid(
                    validation.getNormalizedSql(),
                    semanticValidation.getMessage(),
                    AiQueryHistory.ExecutionStatus.SEMANTIC_VALIDATION_FAILED
            );
        }

        return ValidatedSql.valid(validation.getNormalizedSql());
    }

    private String generateSql(
            String question,
            String schema,
            String businessRules,
            String classificationResult,
            String fewShotExamples,
            String currentTime,
            String retryReason
    ) {
        return sqlSanitizer.sanitize(sqlAssistant.generateSql(
                schema,
                businessRules,
                classificationResult,
                fewShotExamples,
                question,
                currentTime,
                retryReason
        ));
    }

    private String resolveValidationFailureAnswer(ValidatedSql validatedSql) {
        if (validatedSql.getStatus() == AiQueryHistory.ExecutionStatus.SEMANTIC_VALIDATION_FAILED) {
            return SEMANTIC_VALIDATION_FAILED_ANSWER;
        }
        return BLOCKED_SQL_ANSWER;
    }

    private String toJson(List<Map<String, Object>> rows) {
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private String toJson(AiChartResponse chart) {
        try {
            return objectMapper.writeValueAsString(chart);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String toJson(AiIntentResult intentResult, AiQuestionInterpretation interpretation) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "classification", intentResult,
                    "interpretation", interpretation
            ));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private AiQueryHistory saveHistory(
            User worker,
            QuestionContext questionContext,
            String generatedSql,
            String answer,
            String resultJson,
            String chartSpecJson,
            Integer rowCount,
            Long executionTimeMs,
            AiQueryHistory.ExecutionStatus status,
            String errorLog
    ) {
        AiQueryHistory history = new AiQueryHistory();
        history.setWorker(worker);
        history.setNaturalQuestion(questionContext.getOriginalQuestion());
        history.setConversationId(questionContext.getConversationId());
        history.setParentQueryId(questionContext.getParentQueryId());
        history.setEffectiveQuestion(questionContext.getEffectiveQuestion());
        history.setGeneratedSql(generatedSql);
        history.setNaturalAnswer(answer);
        history.setResultJson(resultJson);
        history.setChartSpecJson(chartSpecJson);
        history.setRowCount(rowCount);
        history.setExecutionTimeMs(executionTimeMs);
        history.setModelName(modelName);
        history.setExecutionStatus(status);
        history.setErrorLog(errorLog);
        AiQueryHistory savedHistory = aiQueryHistoryRepository.save(history);
        completeParentClarification(questionContext);
        return savedHistory;
    }

    private void completeParentClarification(QuestionContext questionContext) {
        if (questionContext.getParentQueryId() == null) {
            return;
        }
        aiQueryHistoryRepository.findById(questionContext.getParentQueryId())
                .filter(history -> history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED)
                .ifPresent(history -> history.setExecutionStatus(AiQueryHistory.ExecutionStatus.CLARIFICATION_ANSWERED));
    }

    private AiQueryResponse toResponse(
            AiQueryHistory history,
            List<Map<String, Object>> rows,
            AiQuestionInterpretation interpretation
    ) {
        AiQueryResponse response = new AiQueryResponse();
        response.setQueryId(history.getQueryId());
        response.setQuestion(history.getNaturalQuestion());
        response.setConversationId(history.getConversationId());
        response.setClarificationOfQueryId(history.getParentQueryId());
        response.setPendingClarificationQueryId(resolvePendingClarificationQueryId(history));
        response.setEffectiveQuestion(history.getEffectiveQuestion());
        response.setGeneratedSql(history.getGeneratedSql());
        response.setRows(rows);
        response.setRowCount(history.getRowCount());
        response.setExecutionStatus(history.getExecutionStatus());
        response.setChart(resolveChart(history));
        response.setClarificationRequired(history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED);
        applyInterpretationResponse(response, interpretation);
        response.setAnswerType(resolveAnswerType(history));
        if (Boolean.TRUE.equals(response.getClarificationRequired())) {
            response.setAnswer(CLARIFICATION_REQUIRED_ANSWER);
            response.setClarificationQuestion(history.getNaturalAnswer());
        } else {
            response.setAnswer(history.getNaturalAnswer());
        }
        return response;
    }

    private void applyInterpretationResponse(AiQueryResponse response, AiQuestionInterpretation interpretation) {
        if (interpretation == null) {
            response.setInterpretedDomain("unknown");
            response.setInterpretedIntent("unknown");
            response.setInterpretationSummary("");
            response.setSuggestedQuestions(List.of());
            return;
        }
        response.setInterpretedDomain(interpretation.getDomain());
        response.setInterpretedIntent(interpretation.getIntent());
        response.setInterpretationSummary(interpretation.getSummary());
        response.setSuggestedQuestions(interpretation.getSuggestedQuestions());
    }

    private String resolveAnswerType(AiQueryHistory history) {
        if (history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.SUCCESS) {
            return "NORMAL";
        }
        if (history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED) {
            return "CLARIFICATION";
        }
        if (history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.NOT_DATA_QUESTION) {
            return "UNSUPPORTED";
        }
        if (history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.SEMANTIC_VALIDATION_FAILED) {
            return "CLARIFICATION";
        }
        return "ERROR";
    }

    private Integer resolvePendingClarificationQueryId(AiQueryHistory history) {
        if (history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED) {
            return history.getQueryId();
        }
        return null;
    }

    private AiChartResponse resolveChart(AiQueryHistory history) {
        if (history.getChartSpecJson() == null || history.getChartSpecJson().isBlank()) {
            return AiChartResponse.none("차트로 표현할 수 있는 데이터가 없습니다.");
        }

        try {
            return objectMapper.readValue(history.getChartSpecJson(), AiChartResponse.class);
        } catch (JsonProcessingException exception) {
            return AiChartResponse.none("차트 추천 결과를 해석할 수 없습니다.");
        }
    }

    private long elapsedTime(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private static class QuestionContext {

        private String originalQuestion;
        private String effectiveQuestion;
        private String conversationId;
        private Integer parentQueryId;

        static QuestionContext of(String originalQuestion, String effectiveQuestion, String conversationId, Integer parentQueryId) {
            QuestionContext context = new QuestionContext();
            context.originalQuestion = originalQuestion;
            context.effectiveQuestion = effectiveQuestion;
            context.conversationId = conversationId;
            context.parentQueryId = parentQueryId;
            return context;
        }

        String getOriginalQuestion() {
            return originalQuestion;
        }

        String getEffectiveQuestion() {
            return effectiveQuestion;
        }

        String getConversationId() {
            return conversationId;
        }

        Integer getParentQueryId() {
            return parentQueryId;
        }
    }

    private static class ValidatedSql {

        private boolean valid;
        private String sql;
        private String normalizedSql;
        private String message;
        private AiQueryHistory.ExecutionStatus status;

        static ValidatedSql valid(String normalizedSql) {
            ValidatedSql result = new ValidatedSql();
            result.valid = true;
            result.sql = normalizedSql;
            result.normalizedSql = normalizedSql;
            result.message = "success";
            result.status = AiQueryHistory.ExecutionStatus.SUCCESS;
            return result;
        }

        static ValidatedSql invalid(String sql, String message, AiQueryHistory.ExecutionStatus status) {
            ValidatedSql result = new ValidatedSql();
            result.valid = false;
            result.sql = sql;
            result.normalizedSql = sql;
            result.message = message;
            result.status = status;
            return result;
        }

        boolean isValid() {
            return valid;
        }

        String getSql() {
            return sql;
        }

        String getNormalizedSql() {
            return normalizedSql;
        }

        String getMessage() {
            return message;
        }

        AiQueryHistory.ExecutionStatus getStatus() {
            return status;
        }
    }
}

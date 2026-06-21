package com.ssafy.demo_app.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
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
import com.ssafy.demo_app.domain.ai.service.prompt.DataQuestionCandidateService;
import com.ssafy.demo_app.domain.ai.service.prompt.FewShotPromptService;
import com.ssafy.demo_app.domain.ai.service.rule.BusinessRulePromptService;
import com.ssafy.demo_app.domain.ai.service.schema.DatabaseSchemaService;
import com.ssafy.demo_app.domain.ai.service.sql.SqlExecutionService;
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

    private final UserRepository userRepository;
    private final AiQueryHistoryRepository aiQueryHistoryRepository;
    private final DatabaseSchemaService databaseSchemaService;
    private final BusinessRulePromptService businessRulePromptService;
    private final IntentClassificationService intentClassificationService;
    private final DataQuestionCandidateService dataQuestionCandidateService;
    private final SqlAssistant sqlAssistant;
    private final FewShotPromptService fewShotPromptService;
    private final ClarificationService clarificationService;
    private final SqlSanitizer sqlSanitizer;
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
    public AiQueryResponse ask(Integer userId, String question) {
        User worker = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        long startedAt = System.currentTimeMillis();
        String currentTime = LocalDateTime.now().format(CURRENT_TIME_FORMATTER);
        String schema;
        try {
            schema = databaseSchemaService.getSchemaDescription();
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    null,
                    SCHEMA_LOAD_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SCHEMA_LOAD_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of());
        }

        String businessRules = businessRulePromptService.getBusinessRules();
        AiIntentResult intentResult;
        try {
            intentResult = intentClassificationService.classify(question, schema, businessRules, currentTime);
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    null,
                    SQL_GENERATION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of());
        }

        boolean dataQuestionCandidate = dataQuestionCandidateService.isCandidate(question);
        if (!intentResult.isDataQuestion() && !dataQuestionCandidate) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    null,
                    NOT_DATA_QUESTION_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.NOT_DATA_QUESTION,
                    null
            );
            return toResponse(history, List.of());
        }
        if (!intentResult.isDataQuestion()) {
            intentResult.setDataQuestion(true);
            intentResult.setReason("도메인 키워드 기반 데이터 질의 후보로 판단했습니다.");
        }
        if (intentResult.isNeedsClarification() && !intentResult.getClarificationQuestion().isBlank()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    null,
                    intentResult.getClarificationQuestion(),
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED,
                    null
            );
            return toResponse(history, List.of());
        }

        String fewShotExamples;
        try {
            fewShotExamples = fewShotPromptService.getFewShotExamples();
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    null,
                    SQL_GENERATION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of());
        }

        ClarificationResult clarification = clarificationService.evaluate(question, schema, currentTime);
        if (clarification.isClarificationRequired()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    null,
                    clarification.getQuestion(),
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED,
                    null
            );
            return toResponse(history, List.of());
        }

        String classificationResult = toJson(intentResult);
        ValidatedSql validatedSql;
        try {
            validatedSql = generateValidatedSql(
                    question,
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
                    question,
                    null,
                    SQL_GENERATION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of());
        }

        if (!validatedSql.isValid()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    validatedSql.getSql(),
                    resolveValidationFailureAnswer(validatedSql),
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    validatedSql.getStatus(),
                    validatedSql.getMessage()
            );
            return toResponse(history, List.of());
        }

        List<Map<String, Object>> rows;
        try {
            rows = sqlExecutionService.execute(validatedSql.getNormalizedSql());
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    validatedSql.getNormalizedSql(),
                    SQL_EXECUTION_FAILED_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.SQL_EXECUTION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, List.of());
        }

        String resultJson = toJson(rows);
        String answer;
        try {
            answer = answerGenerator.generateAnswer(
                    question,
                    classificationResult,
                    businessRules,
                    validatedSql.getNormalizedSql(),
                    resultJson
            );
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    validatedSql.getNormalizedSql(),
                    ANSWER_GENERATION_FAILED_ANSWER,
                    resultJson,
                    null,
                    rows.size(),
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.ANSWER_GENERATION_FAILED,
                    exception.getMessage()
            );
            return toResponse(history, rows);
        }

        AiChartResponse chart = chartRecommendationService.recommend(question, rows);
        String chartSpecJson = toJson(chart);
        AiQueryHistory history = saveHistory(
                worker,
                question,
                validatedSql.getNormalizedSql(),
                answer,
                resultJson,
                chartSpecJson,
                rows.size(),
                elapsedTime(startedAt),
                AiQueryHistory.ExecutionStatus.SUCCESS,
                null
        );
        return toResponse(history, rows);
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
        String sql = generateSql(question, schema, businessRules, classificationResult, fewShotExamples, currentTime, "");
        ValidatedSql firstResult = validateSql(question, intentResult, sql);
        if (firstResult.isValid()) {
            return firstResult;
        }

        String retriedSql = generateSql(
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

    private String toJson(AiIntentResult intentResult) {
        try {
            return objectMapper.writeValueAsString(intentResult);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private AiQueryHistory saveHistory(
            User worker,
            String question,
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
        history.setNaturalQuestion(question);
        history.setGeneratedSql(generatedSql);
        history.setNaturalAnswer(answer);
        history.setResultJson(resultJson);
        history.setChartSpecJson(chartSpecJson);
        history.setRowCount(rowCount);
        history.setExecutionTimeMs(executionTimeMs);
        history.setModelName(modelName);
        history.setExecutionStatus(status);
        history.setErrorLog(errorLog);
        return aiQueryHistoryRepository.save(history);
    }

    private AiQueryResponse toResponse(AiQueryHistory history, List<Map<String, Object>> rows) {
        AiQueryResponse response = new AiQueryResponse();
        response.setQueryId(history.getQueryId());
        response.setQuestion(history.getNaturalQuestion());
        response.setGeneratedSql(history.getGeneratedSql());
        response.setRows(rows);
        response.setRowCount(history.getRowCount());
        response.setExecutionStatus(history.getExecutionStatus());
        response.setChart(resolveChart(history));
        response.setClarificationRequired(history.getExecutionStatus() == AiQueryHistory.ExecutionStatus.CLARIFICATION_REQUIRED);
        if (Boolean.TRUE.equals(response.getClarificationRequired())) {
            response.setAnswer(CLARIFICATION_REQUIRED_ANSWER);
            response.setClarificationQuestion(history.getNaturalAnswer());
        } else {
            response.setAnswer(history.getNaturalAnswer());
        }
        return response;
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

package com.ssafy.demo_app.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.api.ai.dto.AiChartResponse;
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
import java.util.Locale;
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
    private static final String SQL_EXECUTION_FAILED_ANSWER = "쿼리 실행 중 오류가 발생했습니다.";
    private static final String ANSWER_GENERATION_FAILED_ANSWER = "답변 생성 중 오류가 발생했습니다.";

    private final UserRepository userRepository;
    private final AiQueryHistoryRepository aiQueryHistoryRepository;
    private final DatabaseSchemaService databaseSchemaService;
    private final IntentClassifier intentClassifier;
    private final DataQuestionCandidateService dataQuestionCandidateService;
    private final SqlAssistant sqlAssistant;
    private final FewShotPromptService fewShotPromptService;
    private final ClarificationService clarificationService;
    private final SqlSanitizer sqlSanitizer;
    private final SqlValidationService sqlValidationService;
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

        String intent;
        try {
            intent = classifyIntent(question, schema, currentTime);
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
        if (!intent.contains("YES") && !dataQuestionCandidate) {
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

        String sql;
        try {
            sql = generateSql(question, schema, fewShotExamples, currentTime);
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

        SqlValidationResult validation = sqlValidationService.validate(sql);
        if (!validation.isValid()) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    sql,
                    BLOCKED_SQL_ANSWER,
                    null,
                    null,
                    0,
                    elapsedTime(startedAt),
                    AiQueryHistory.ExecutionStatus.BLOCKED_UNSAFE_SQL,
                    validation.getMessage()
            );
            return toResponse(history, List.of());
        }

        List<Map<String, Object>> rows;
        try {
            rows = sqlExecutionService.execute(validation.getNormalizedSql());
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    validation.getNormalizedSql(),
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
            answer = answerGenerator.generateAnswer(question, resultJson);
        } catch (Exception exception) {
            AiQueryHistory history = saveHistory(
                    worker,
                    question,
                    validation.getNormalizedSql(),
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
                validation.getNormalizedSql(),
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

    private String classifyIntent(String question, String schema, String currentTime) {
        String rawIntent = intentClassifier.classify(question, schema, currentTime);
        if (rawIntent == null) {
            return "";
        }
        return rawIntent.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }

    private String generateSql(String question, String schema, String fewShotExamples, String currentTime) {
        return sqlSanitizer.sanitize(sqlAssistant.generateSql(schema, fewShotExamples, question, currentTime));
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
}

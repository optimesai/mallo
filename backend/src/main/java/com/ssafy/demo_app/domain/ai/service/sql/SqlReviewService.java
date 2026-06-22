package com.ssafy.demo_app.domain.ai.service.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.domain.ai.service.assistant.SqlReviewAssistant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SqlReviewService {

    private final SqlReviewAssistant sqlReviewAssistant;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SqlReviewResult review(
            String question,
            String schema,
            String businessRules,
            String classificationResult,
            String sql
    ) {
        if (sql == null || sql.isBlank()) {
            return SqlReviewResult.invalid("SQL이 비어 있습니다.", "질문에 맞는 SELECT SQL을 다시 생성하세요.");
        }

        try {
            String rawResult = sqlReviewAssistant.review(schema, businessRules, classificationResult, question, sql);
            return parseResult(rawResult);
        } catch (Exception exception) {
            return SqlReviewResult.valid("SQL 검토를 건너뛰고 기존 검증 단계로 진행합니다.");
        }
    }

    private SqlReviewResult parseResult(String rawResult) throws JsonProcessingException {
        if (rawResult == null || rawResult.isBlank()) {
            return SqlReviewResult.valid("SQL 검토 결과가 비어 있어 기존 검증 단계로 진행합니다.");
        }

        SqlReviewResult result = objectMapper.readValue(cleanJson(rawResult), SqlReviewResult.class);
        if (result.isValid()) {
            result.setRetryInstruction("");
            return result;
        }
        if (result.getRetryInstruction() == null || result.getRetryInstruction().isBlank()) {
            result.setRetryInstruction(result.getReason());
        }
        return result;
    }

    private String cleanJson(String rawResult) {
        return rawResult
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    @Getter
    @Setter
    public static class SqlReviewResult {

        private boolean valid;
        private String reason = "";
        private String retryInstruction = "";

        public static SqlReviewResult valid(String reason) {
            SqlReviewResult result = new SqlReviewResult();
            result.setValid(true);
            result.setReason(reason);
            result.setRetryInstruction("");
            return result;
        }

        public static SqlReviewResult invalid(String reason, String retryInstruction) {
            SqlReviewResult result = new SqlReviewResult();
            result.setValid(false);
            result.setReason(reason);
            result.setRetryInstruction(retryInstruction);
            return result;
        }
    }
}

package com.ssafy.demo_app.domain.ai.service.sql;

import com.ssafy.demo_app.domain.ai.service.assistant.SqlReviewAssistant;
import com.ssafy.demo_app.domain.ai.service.sql.SqlReviewService.SqlReviewResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SqlReviewServiceTest {

    private final SqlReviewAssistant sqlReviewAssistant = mock(SqlReviewAssistant.class);
    private final SqlReviewService sqlReviewService = new SqlReviewService(sqlReviewAssistant);

    @Test
    void review_returnsInvalidResultWithRetryInstruction() {
        given(sqlReviewAssistant.review(
                "schema",
                "business rules",
                "classification",
                "question",
                "sql"
        )).willReturn("""
                {
                  "valid": false,
                  "reason": "품목 필터가 item_code만 사용되었습니다.",
                  "retryInstruction": "item_code, item_name, item_id를 모두 고려하세요."
                }
                """);

        SqlReviewResult result = sqlReviewService.review(
                "question",
                "schema",
                "business rules",
                "classification",
                "sql"
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("item_code");
        assertThat(result.getRetryInstruction()).contains("item_name");
    }

    @Test
    void review_fallsBackToValidWhenAssistantFails() {
        given(sqlReviewAssistant.review(
                "schema",
                "business rules",
                "classification",
                "question",
                "sql"
        )).willThrow(new IllegalStateException("review failed"));

        SqlReviewResult result = sqlReviewService.review(
                "question",
                "schema",
                "business rules",
                "classification",
                "sql"
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getReason()).contains("건너뛰고");
    }
}

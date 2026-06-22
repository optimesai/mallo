package com.ssafy.demo_app.domain.ai.entity;

import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_query_history")
@Getter
@Setter
public class AiQueryHistory extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "query_id")
    private Integer queryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Lob
    @Column(name = "natural_question", nullable = false, columnDefinition = "TEXT")
    private String naturalQuestion;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "parent_query_id")
    private Integer parentQueryId;

    @Lob
    @Column(name = "effective_question", columnDefinition = "TEXT")
    private String effectiveQuestion;

    @Lob
    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Lob
    @Column(name = "natural_answer", columnDefinition = "TEXT")
    private String naturalAnswer;

    @Lob
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Lob
    @Column(name = "chart_spec_json", columnDefinition = "TEXT")
    private String chartSpecJson;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "model_name")
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;

    @Lob
    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    public enum ExecutionStatus {
        SUCCESS,
        NOT_DATA_QUESTION,
        SQL_GENERATION_FAILED,
        BLOCKED_UNSAFE_SQL,
        SQL_EXECUTION_FAILED,
        ANSWER_GENERATION_FAILED,
        SCHEMA_LOAD_FAILED,
        CLARIFICATION_REQUIRED,
        SEMANTIC_VALIDATION_FAILED,
        TIMEOUT
    }
}

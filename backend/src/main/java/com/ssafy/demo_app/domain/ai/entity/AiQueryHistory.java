package com.ssafy.demo_app.domain.ai.entity;

import com.ssafy.demo_app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_query_history")
@Getter
@Setter
public class AiQueryHistory {

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

    @Lob
    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;

    @Lob
    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ExecutionStatus {
        SUCCESS,
        BLOCKED_DML,
        SYNTAX_ERROR,
        TIMEOUT
    }
}

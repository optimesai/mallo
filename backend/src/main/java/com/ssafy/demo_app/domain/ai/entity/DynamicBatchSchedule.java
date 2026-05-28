package com.ssafy.demo_app.domain.ai.entity;

import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_batch_schedule")
@Getter
@Setter
public class DynamicBatchSchedule extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "schedule_name", nullable = false)
    private String scheduleName;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private AiQueryHistory query;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_run_status", nullable = false)
    private LastRunStatus lastRunStatus = LastRunStatus.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    public enum LastRunStatus {
        SUCCESS,
        FAILED,
        NONE
    }
}

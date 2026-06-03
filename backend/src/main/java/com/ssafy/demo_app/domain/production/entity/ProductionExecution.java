package com.ssafy.demo_app.domain.production.entity;

import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "production_execution")
@Getter
@Setter
public class ProductionExecution extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Integer executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private WorkOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_id")
    private FactoryRouting routing;

    @Column(name = "good_qty", nullable = false)
    private Integer goodQty = 0;

    @Column(name = "defect_qty", nullable = false)
    private Integer defectQty = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Column(name = "man_hours_minutes", nullable = false)
    private Integer manHoursMinutes;
}

package com.ssafy.demo_app.domain.production.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "work_order_sequence", uniqueConstraints = {
        @UniqueConstraint(columnNames = "plan_date")
})
@Getter
@Setter
public class WorkOrderSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id")
    private Integer sequenceId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence = 0;
}

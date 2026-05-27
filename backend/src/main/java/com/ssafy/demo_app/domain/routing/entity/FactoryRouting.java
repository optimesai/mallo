package com.ssafy.demo_app.domain.routing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "factory_routing", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"factory_name", "line_name", "operation_seq"})
})
@Getter
@Setter
public class FactoryRouting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routing_id")
    private Integer routingId;

    @Column(name = "factory_name", nullable = false)
    private String factoryName;

    @Column(name = "line_name", nullable = false)
    private String lineName;

    @Column(name = "operation_seq", nullable = false)
    private Integer operationSeq;

    @Column(name = "operation_name", nullable = false)
    private String operationName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

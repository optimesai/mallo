package com.ssafy.demo_app.domain.production.entity;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_order")
@Getter
@Setter
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_id", nullable = false)
    private FactoryRouting routing;

    @Column(name = "target_qty", nullable = false)
    private Integer targetQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.READY;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        READY,
        RUN,
        HOLD,
        CLOSE
    }
}

package com.ssafy.demo_app.domain.inventory.entity;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbound_receipt")
@Getter
@Setter
public class InboundReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_id")
    private Integer inboundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerMaster partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "inbound_qty", nullable = false)
    private Integer inboundQty;

    @Column(name = "inbound_date", nullable = false)
    private LocalDate inboundDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboundStatus status = InboundStatus.READY;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum InboundStatus {
        READY,
        COMPLETED
    }
}

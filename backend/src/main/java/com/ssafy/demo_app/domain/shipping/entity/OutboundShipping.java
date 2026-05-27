package com.ssafy.demo_app.domain.shipping.entity;

import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_shipping")
@Getter
@Setter
public class OutboundShipping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_id")
    private Integer shippingId;

    @Column(name = "shipping_no", nullable = false, unique = true)
    private String shippingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerMaster partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    @Column(name = "request_qty", nullable = false)
    private Integer requestQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picking_location_id")
    private WarehouseLocation pickingLocation;

    @Column(name = "vehicle_no")
    private String vehicleNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShippingStatus status = ShippingStatus.READY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ShippingStatus {
        READY,
        PICKING,
        SHIPPED
    }
}

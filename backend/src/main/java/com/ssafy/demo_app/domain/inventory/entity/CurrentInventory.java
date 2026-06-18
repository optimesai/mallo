package com.ssafy.demo_app.domain.inventory.entity;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "current_inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"item_id", "location_id", "lot_number"})
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class CurrentInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Integer inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "current_qty", nullable = false)
    private Integer currentQty = 0;

    @Column(name = "first_inbound_date")
    private LocalDate firstInboundDate;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

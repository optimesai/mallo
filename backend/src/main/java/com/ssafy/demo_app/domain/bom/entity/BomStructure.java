package com.ssafy.demo_app.domain.bom.entity;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bom_structure", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_item_id", "child_item_id", "bom_version"})
})
@Getter
@Setter
public class BomStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bom_id")
    private Integer bomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private ItemMaster parentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_item_id", nullable = false)
    private ItemMaster childItem;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;

    @Column(name = "bom_version", nullable = false)
    private String bomVersion = "v1.0";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

package com.ssafy.demo_app.domain.bom.entity;

import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bom_structure", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_item_id", "child_item_id", "bom_version"})
})
@Getter
@Setter
public class BomStructure extends BaseCreatedTimeEntity {

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

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "bom_version", nullable = false)
    private String bomVersion = "v1.0";

    @Enumerated(EnumType.STRING)
    @Column(name = "bom_status", nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
    private BomStatus bomStatus = BomStatus.ACTIVE;

    public enum BomStatus {
        ACTIVE,
        INACTIVE
    }
}

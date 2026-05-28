package com.ssafy.demo_app.domain.item.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.ssafy.demo_app.global.common.BaseCreatedTimeEntity;

@Entity
@Table(name = "item_master")
@Getter
@Setter
public class ItemMaster extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_code", nullable = false, unique = true)
    private String itemCode;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "spec")
    private String spec;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock = 0;

    public enum Unit {
        ea, kg, box, L
    }

    public enum ItemType {
        RAW, HALF, FG
    }
}

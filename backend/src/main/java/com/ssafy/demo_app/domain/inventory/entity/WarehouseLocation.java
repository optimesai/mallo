package com.ssafy.demo_app.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "warehouse_location")
@Getter
@Setter
public class WarehouseLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Integer locationId;

    @Column(name = "location_code", nullable = false, unique = true)
    private String locationCode;

    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "rack_row")
    private String rackRow;

    @Column(name = "rack_column")
    private String rackColumn;
}

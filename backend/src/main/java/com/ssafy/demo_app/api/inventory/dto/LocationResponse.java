package com.ssafy.demo_app.api.inventory.dto;

import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationResponse {

    private Integer locationId;
    private String locationCode;
    private String warehouseName;
    private String rackRow;
    private String rackColumn;

    public static LocationResponse from(WarehouseLocation location) {
        LocationResponse response = new LocationResponse();
        response.setLocationId(location.getLocationId());
        response.setLocationCode(location.getLocationCode());
        response.setWarehouseName(location.getWarehouseName());
        response.setRackRow(location.getRackRow());
        response.setRackColumn(location.getRackColumn());
        return response;
    }
}

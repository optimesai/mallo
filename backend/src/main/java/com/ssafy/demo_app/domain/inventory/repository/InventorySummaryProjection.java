package com.ssafy.demo_app.domain.inventory.repository;

import java.time.LocalDateTime;

public interface InventorySummaryProjection {

    Integer getInventoryId();

    String getItemCode();

    String getItemName();

    String getLocationCode();

    String getWarehouseName();

    Integer getCurrentQty();

    LocalDateTime getUpdatedAt();
}

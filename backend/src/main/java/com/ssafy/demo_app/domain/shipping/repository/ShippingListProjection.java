package com.ssafy.demo_app.domain.shipping.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ShippingListProjection {

    Integer getShippingId();

    String getShippingNo();

    String getPartnerCode();

    String getPartnerName();

    String getItemCode();

    String getItemName();

    Integer getRequestQty();

    Integer getShippedQty();

    String getShippingType();

    String getPickingLocationCode();

    String getVehicleNo();

    String getCarrier();

    String getTrackingNo();

    LocalDate getEstimatedDelivery();

    String getCancelReason();

    String getStatus();

    String getWorkerName();

    LocalDateTime getShippedAt();
}

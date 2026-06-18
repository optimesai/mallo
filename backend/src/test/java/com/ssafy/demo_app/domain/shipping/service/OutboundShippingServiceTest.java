package com.ssafy.demo_app.domain.shipping.service;
import com.ssafy.demo_app.domain.inventory.entity.TransactionType;

import com.ssafy.demo_app.api.shipping.dto.ShippingCreateRequest;
import com.ssafy.demo_app.api.shipping.dto.ShippingResponse;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InventoryTransactionHistory;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.shipping.entity.OutboundShipping;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.demo_app.global.response.PageResponse;

import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OutboundShippingServiceTest {

    @Autowired
    private OutboundShippingService outboundShippingService;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("출하 지시 등록 및 조회 성공 검증")
    void registerAndGetShipping_success() {
        // Given
        PartnerMaster partner = em.createQuery(
                        "select p from PartnerMaster p where p.partnerType = :partnerType",
                        PartnerMaster.class
                )
                .setParameter("partnerType", PartnerMaster.PartnerType.CUSTOMER)
                .getResultList()
                .get(0);
        ItemMaster item = createItem("SH-ITEM-01", "완제품A", ItemMaster.ItemType.FG);

        ShippingCreateRequest request = new ShippingCreateRequest();
        request.setShippingNo("SH-2026-TEST-999");
        request.setPartnerCode(partner.getPartnerCode());
        request.setItemCode(item.getItemCode());
        request.setRequestQty(50);

        // When
        ShippingResponse response = outboundShippingService.registerShipping(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShippingNo()).isEqualTo("SH-2026-TEST-999");
        assertThat(response.getStatus()).isEqualTo(OutboundShipping.ShippingStatus.READY.name());

        // 목록 조회 확인
        PageResponse<ShippingResponse> shippings = outboundShippingService.getShippings(Pageable.unpaged(), null, null);
        assertThat(shippings.getContent()).isNotEmpty();
        assertThat(shippings.getContent().stream().anyMatch(s -> s.getShippingNo().equals("SH-2026-TEST-999"))).isTrue();

        // 단건 조회 확인
        ShippingResponse detail = outboundShippingService.getShipping(response.getShippingId());
        assertThat(detail.getShippingNo()).isEqualTo("SH-2026-TEST-999");
    }

    @Test
    @DisplayName("출하 완료 성공 - 재고 차감 및 수불 이력 생성 검증")
    void completeShipping_success() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-02", "완제품B", ItemMaster.ItemType.FG);

        // 재고 설정 (현재고 100개)
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(100);
        em.persist(inventory);

        // 출하 지시 생성 및 피킹 위치 배정 (READY 상태)
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-777");
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(40);
        shipping.setPickingLocation(location);
        shipping.setStatus(OutboundShipping.ShippingStatus.PICKING);
        em.persist(shipping);

        em.flush();
        em.clear();

        // When
        outboundShippingService.completeShipping(shipping.getShippingId(), worker.getUserId());

        // Then
        // 1. 재고 차감 확인 — inventory deducted at PICKING stage, not at completeShipping
        // 2. 출하 지시 상태가 SHIPPED로 변경되었는지 확인
        OutboundShipping updatedShipping = em.find(OutboundShipping.class, shipping.getShippingId());
        assertThat(updatedShipping.getStatus()).isEqualTo(OutboundShipping.ShippingStatus.SHIPPED);
        assertThat(updatedShipping.getShippedAt()).isNotNull();
        assertThat(updatedShipping.getWorker().getUserId()).isEqualTo(worker.getUserId());

        // 3. 수불 이력 생성 확인 (OUTBOUND)
        List<InventoryTransactionHistory> histories = em.createQuery(
                "select h from InventoryTransactionHistory h where h.item = :item", InventoryTransactionHistory.class)
                .setParameter("item", item)
                .getResultList();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getTransactionType()).isEqualTo(TransactionType.OUTBOUND);
        assertThat(histories.get(0).getQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("출하 완료 성공 - 재고는 PICKING 단계에서 이미 차감되었으므로 완료 시 재고 확인 불필요")
    void completeShipping_success_afterPicking() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-03", "완제품C", ItemMaster.ItemType.FG);

        // 출하 지시 생성 (PICKING 상태, 재고는 이미 PICKING 단계에서 차감됨)
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-888");
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(30);
        shipping.setPickingLocation(location);
        shipping.setStatus(OutboundShipping.ShippingStatus.PICKING);
        em.persist(shipping);

        em.flush();
        em.clear();

        // When & Then — INSUFFICIENT_STOCK이 발생하지 않고 정상 완료되어야 함
        outboundShippingService.completeShipping(shipping.getShippingId(), worker.getUserId());

        OutboundShipping updated = em.find(OutboundShipping.class, shipping.getShippingId());
        assertThat(updated.getStatus()).isEqualTo(OutboundShipping.ShippingStatus.SHIPPED);
        assertThat(updated.getShippedAt()).isNotNull();
    }

    @Test
    @DisplayName("출하 완료 실패 - READY 상태인 경우 SHIPPING_STATUS_INVALID 예외 발생")
    void completeShipping_readyStatus_fail() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-03-R", "완제품CR", ItemMaster.ItemType.FG);

        // 재고 설정 (현재고 100개)
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(100);
        em.persist(inventory);

        // 출하 지시 생성 및 피킹 위치 배정 (READY 상태)
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-888-R");
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(30);
        shipping.setPickingLocation(location);
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);
        em.persist(shipping);

        em.flush();
        em.clear();

        // When & Then
        assertThatThrownBy(() -> outboundShippingService.completeShipping(shipping.getShippingId(), worker.getUserId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHIPPING_STATUS_INVALID);
    }

    @Test
    @DisplayName("출하 완료 실패 - 이미 SHIPPED 상태인 경우 SHIPPING_STATUS_INVALID 예외 발생")
    void completeShipping_shippedStatus_fail() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-03-S", "완제품CS", ItemMaster.ItemType.FG);

        // 재고 설정 (현재고 100개)
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(100);
        em.persist(inventory);

        // 출하 지시 생성 및 피킹 위치 배정 (SHIPPED 상태)
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-888-S");
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(30);
        shipping.setPickingLocation(location);
        shipping.setStatus(OutboundShipping.ShippingStatus.SHIPPED);
        em.persist(shipping);

        em.flush();
        em.clear();

        // When & Then
        assertThatThrownBy(() -> outboundShippingService.completeShipping(shipping.getShippingId(), worker.getUserId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHIPPING_STATUS_INVALID);
    }

    @Test
    @DisplayName("피킹 지시 및 차량 배정 성공 검증")
    void assignPicking_success() {
        // Given
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-04", "완제품D", ItemMaster.ItemType.FG);

        // 재고 설정 (현재고 80개)
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(80);
        em.persist(inventory);

        // 출하 지시 생성 (READY 상태, 요청량 50개)
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-666");
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(50);
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);
        em.persist(shipping);

        em.flush();
        em.clear();

        com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest request = new com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest("서울 88 가 9999");

        // When
        ShippingResponse response = outboundShippingService.assignPicking(shipping.getShippingId(), request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVehicleNo()).isEqualTo("서울 88 가 9999");
        assertThat(response.getStatus()).isEqualTo(OutboundShipping.ShippingStatus.PICKING.name());
        assertThat(response.getPickingLocationCode()).isEqualTo(location.getLocationCode());
    }

    @Test
    @DisplayName("피킹 지시 및 차량 배정 실패 - 가용 재고 부족 시 예외 발생")
    void assignPicking_insufficientStock() {
        // Given
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-05", "완제품E", ItemMaster.ItemType.FG);

        // 재고 설정 (현재고 20개)
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(20);
        em.persist(inventory);

        // 출하 지시 생성 (READY 상태, 요청량 50개) -> 재고 부족
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-555");
        shipping.setPartner(partner);
        shipping.setItem(item);
        shipping.setRequestQty(50);
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);
        em.persist(shipping);

        em.flush();
        em.clear();

        com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest request = new com.ssafy.demo_app.api.shipping.dto.PickingAssignRequest("서울 88 가 9999");

        // When & Then
        assertThatThrownBy(() -> outboundShippingService.assignPicking(shipping.getShippingId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
    }

    private ItemMaster createItem(String code, String name, ItemMaster.ItemType type) {
        ItemMaster item = new ItemMaster();
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemType(type);
        item.setUnit(ItemMaster.Unit.ea);
        item.setSafetyStock(10);
        em.persist(item);
        return item;
    }
}

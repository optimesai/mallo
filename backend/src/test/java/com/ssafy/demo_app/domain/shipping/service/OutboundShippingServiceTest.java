package com.ssafy.demo_app.domain.shipping.service;

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
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
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
        List<ShippingResponse> shippings = outboundShippingService.getShippings();
        assertThat(shippings).isNotEmpty();
        assertThat(shippings.stream().anyMatch(s -> s.getShippingNo().equals("SH-2026-TEST-999"))).isTrue();

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
        shipping.setStatus(OutboundShipping.ShippingStatus.READY);
        em.persist(shipping);

        em.flush();
        em.clear();

        // When
        outboundShippingService.completeShipping(shipping.getShippingId(), worker.getUserId());

        // Then
        // 1. 재고 차감 확인 (100 - 40 = 60)
        CurrentInventory updatedInv = em.createQuery(
                "select ci from CurrentInventory ci where ci.item = :item and ci.location = :location", CurrentInventory.class)
                .setParameter("item", item)
                .setParameter("location", location)
                .getSingleResult();
        assertThat(updatedInv.getCurrentQty()).isEqualTo(60);

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
        assertThat(histories.get(0).getTransactionType()).isEqualTo(InventoryTransactionHistory.TransactionType.OUTBOUND);
        assertThat(histories.get(0).getQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("출하 완료 실패 - 완제품 재고 부족 시 INSUFFICIENT_STOCK 예외 발생")
    void completeShipping_insufficientStock() {
        // Given
        User worker = em.createQuery("select u from User u", User.class).getResultList().get(0);
        PartnerMaster partner = em.createQuery("select p from PartnerMaster p", PartnerMaster.class).getResultList().get(0);
        WarehouseLocation location = em.createQuery("select l from WarehouseLocation l", WarehouseLocation.class).getResultList().get(0);
        ItemMaster item = createItem("SH-ITEM-03", "완제품C", ItemMaster.ItemType.FG);

        // 재고 설정 (현재고 10개)
        CurrentInventory inventory = new CurrentInventory();
        inventory.setItem(item);
        inventory.setLocation(location);
        inventory.setCurrentQty(10);
        em.persist(inventory);

        // 출하 지시 생성 및 피킹 위치 배정 (READY 상태, 요청량 30개)
        OutboundShipping shipping = new OutboundShipping();
        shipping.setShippingNo("SH-2026-TEST-888");
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

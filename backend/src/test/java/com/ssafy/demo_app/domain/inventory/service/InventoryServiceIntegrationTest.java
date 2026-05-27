package com.ssafy.demo_app.domain.inventory.service;

import com.ssafy.demo_app.api.inventory.dto.InboundCreateRequest;
import com.ssafy.demo_app.api.inventory.dto.InboundReceiptResponse;
import com.ssafy.demo_app.api.inventory.dto.InventoryStackRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationRequest;
import com.ssafy.demo_app.api.inventory.dto.LocationResponse;
import com.ssafy.demo_app.domain.inventory.entity.CurrentInventory;
import com.ssafy.demo_app.domain.inventory.entity.InboundReceipt;
import com.ssafy.demo_app.domain.inventory.entity.WarehouseLocation;
import com.ssafy.demo_app.domain.inventory.repository.CurrentInventoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.inventory.repository.InventoryTransactionHistoryRepository;
import com.ssafy.demo_app.domain.inventory.repository.WarehouseLocationRepository;
import com.ssafy.demo_app.domain.item.entity.ItemMaster;
import com.ssafy.demo_app.domain.item.repository.ItemMasterRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemMasterRepository itemMasterRepository;

    @Autowired
    private PartnerMasterRepository partnerMasterRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    @Autowired
    private InboundReceiptRepository inboundReceiptRepository;

    @Autowired
    private CurrentInventoryRepository currentInventoryRepository;

    @Autowired
    private InventoryTransactionHistoryRepository transactionHistoryRepository;

    @Test
    @DisplayName("통합 테스트: 입고 등록 -> 완료 -> 재고 적재 흐름 검증")
    void inbound_Complete_And_Stack_Flow() {
        // 1. 기초 데이터 확인 및 준비
        User worker = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("데이터베이스에 사용자가 존재하지 않습니다."));
        ItemMaster item = itemMasterRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("데이터베이스에 품목이 존재하지 않습니다."));
        PartnerMaster partner = partnerMasterRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("데이터베이스에 거래처가 존재하지 않습니다."));
        WarehouseLocation location = warehouseLocationRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("데이터베이스에 로케이션이 존재하지 않습니다."));

        // 2. 입고 등록 (Register Inbound)
        InboundCreateRequest createRequest = new InboundCreateRequest();
        createRequest.setItemCode(item.getItemCode());
        createRequest.setPartnerCode(partner.getPartnerCode());
        createRequest.setLocationCode(location.getLocationCode());
        createRequest.setInboundQty(150);
        createRequest.setInboundDate(LocalDate.now());

        InboundReceiptResponse registerResponse = inventoryService.registerInbound(worker.getUserId(), createRequest);
        assertThat(registerResponse).isNotNull();
        assertThat(registerResponse.getInboundId()).isNotNull();
        assertThat(registerResponse.getStatus()).isEqualTo(InboundReceipt.InboundStatus.READY.name());

        // 3. 입고 완료 처리 (Complete Inbound)
        InboundReceiptResponse completeResponse = inventoryService.completeInbound(registerResponse.getInboundId());
        assertThat(completeResponse.getStatus()).isEqualTo(InboundReceipt.InboundStatus.COMPLETED.name());

        // 4. 재고 적재 (Stack Inventory)
        InventoryStackRequest stackRequest = new InventoryStackRequest();
        stackRequest.setTargetLocationCode(location.getLocationCode());

        int initialQty = currentInventoryRepository.findByItemAndLocation(item, location)
                .map(CurrentInventory::getCurrentQty)
                .orElse(0);

        inventoryService.stackInventory(worker.getUserId(), completeResponse.getInboundId(), stackRequest);

        // 5. 검증: 재고가 정상적으로 누적 증가했는지 검증
        CurrentInventory updatedInventory = currentInventoryRepository.findByItemAndLocation(item, location)
                .orElseThrow(() -> new AssertionError("재고 정보가 생성되지 않았습니다."));
        assertThat(updatedInventory.getCurrentQty()).isEqualTo(initialQty + 150);

        // 6. 검증: 수불 이력(Transaction History) 생성 여부 검증
        boolean historyExists = transactionHistoryRepository.findAll().stream()
                .anyMatch(h -> h.getItem().getItemId().equals(item.getItemId()) 
                        && h.getLocation().getLocationId().equals(location.getLocationId()) 
                        && h.getQuantity() == 150);
        assertThat(historyExists).isTrue();
    }

    @Test
    @DisplayName("통합 테스트: 중복 로케이션 코드 등록 방지 검증")
    void createLocation_DuplicateValidation() {
        // LOC-A-01 코드를 가지는 로케이션 생성
        LocationRequest request1 = new LocationRequest("LOC-TEST-99", "테스트창고", "1", "1");
        inventoryService.createLocation(request1);

        // 동일한 코드로 다시 생성 시도 시 예외 발생 검증
        LocationRequest request2 = new LocationRequest("LOC-TEST-99", "다른창고", "2", "2");
        assertThatThrownBy(() -> inventoryService.createLocation(request2))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOCATION_CODE_DUPLICATE);
    }
}

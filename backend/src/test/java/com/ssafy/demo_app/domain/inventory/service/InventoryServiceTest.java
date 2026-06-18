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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InboundReceiptRepository inboundReceiptRepository;
    @Mock
    private ItemMasterRepository itemMasterRepository;
    @Mock
    private PartnerMasterRepository partnerMasterRepository;
    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentInventoryRepository currentInventoryRepository;
    @Mock
    private InventoryTransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private User worker;
    private ItemMaster item;
    private PartnerMaster partner;
    private WarehouseLocation location;

    @BeforeEach
    void setUp() {
        worker = new User();
        worker.setUserId(1);
        worker.setUserName("홍길동");
        worker.setEmployeeNo("EMP-001");
        worker.setDepartment("물류팀");

        item = new ItemMaster();
        item.setItemId(1);
        item.setItemCode("ITEM-001");
        item.setItemName("원자재A");

        partner = new PartnerMaster();
        partner.setPartnerId(1);
        partner.setPartnerCode("PART-001");
        partner.setPartnerName("공급사A");
        partner.setPartnerType(PartnerMaster.PartnerType.SUPPLIER);
        partner.setPartnerStatus(PartnerMaster.PartnerStatus.ACTIVE);

        location = new WarehouseLocation();
        location.setLocationId(1);
        location.setLocationCode("LOC-A-01");
        location.setWarehouseName("메인창고");
        location.setRackRow("1");
        location.setRackColumn("1");
    }

    @Test
    @DisplayName("원부자재 입고 예정 등록 성공")
    void registerInbound_Success() {
        // given
        InboundCreateRequest request = new InboundCreateRequest();
        request.setItemCode("ITEM-001");
        request.setPartnerCode("PART-001");
        request.setLocationCode("LOC-A-01");
        request.setInboundQty(100);
        request.setInboundDate(LocalDate.now());

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(itemMasterRepository.findByItemCode("ITEM-001")).willReturn(Optional.of(item));
        given(partnerMasterRepository.findByPartnerCode("PART-001")).willReturn(Optional.of(partner));
        given(warehouseLocationRepository.findByLocationCode("LOC-A-01")).willReturn(Optional.of(location));

        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setItem(item);
        receipt.setPartner(partner);
        receipt.setLocation(location);
        receipt.setInboundQty(100);
        receipt.setInboundDate(request.getInboundDate());
        receipt.setWorker(worker);
        receipt.setStatus(InboundReceipt.InboundStatus.READY);

        given(inboundReceiptRepository.save(any(InboundReceipt.class))).willReturn(receipt);

        // when
        InboundReceiptResponse response = inventoryService.registerInbound(1, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getInboundId()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo(InboundReceipt.InboundStatus.READY.name());
        verify(inboundReceiptRepository, times(1)).save(any(InboundReceipt.class));
    }

    @Test
    @DisplayName("원부자재 입고 예정 등록 실패 - 아이템을 찾을 수 없음")
    void registerInbound_ItemNotFound() {
        // given
        InboundCreateRequest request = new InboundCreateRequest();
        request.setItemCode("ITEM-INVALID");

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(itemMasterRepository.findByItemCode("ITEM-INVALID")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.registerInbound(1, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("원부자재 입고 예정 등록 실패 - 거래처를 찾을 수 없음")
    void registerInbound_PartnerNotFound() {
        // given
        InboundCreateRequest request = new InboundCreateRequest();
        request.setItemCode("ITEM-001");
        request.setPartnerCode("PART-INVALID");

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(itemMasterRepository.findByItemCode("ITEM-001")).willReturn(Optional.of(item));
        given(partnerMasterRepository.findByPartnerCode("PART-INVALID")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.registerInbound(1, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNER_NOT_FOUND);
    }

    @Test
    @DisplayName("원부자재 입고 예정 등록 실패 - 로케이션을 찾을 수 없음")
    void registerInbound_LocationNotFound() {
        // given
        InboundCreateRequest request = new InboundCreateRequest();
        request.setItemCode("ITEM-001");
        request.setPartnerCode("PART-001");
        request.setLocationCode("LOC-INVALID");

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(itemMasterRepository.findByItemCode("ITEM-001")).willReturn(Optional.of(item));
        given(partnerMasterRepository.findByPartnerCode("PART-001")).willReturn(Optional.of(partner));
        given(warehouseLocationRepository.findByLocationCode("LOC-INVALID")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.registerInbound(1, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOCATION_NOT_FOUND);
    }

    @Test
    @DisplayName("입고 완료 처리 성공")
    void completeInbound_Success() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setItem(item);
        receipt.setPartner(partner);
        receipt.setLocation(location);
        receipt.setStatus(InboundReceipt.InboundStatus.READY);

        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));
        given(inboundReceiptRepository.save(any(InboundReceipt.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        InboundReceiptResponse response = inventoryService.completeInbound(10);

        // then
        assertThat(response.getStatus()).isEqualTo(InboundReceipt.InboundStatus.COMPLETED.name());
        verify(inboundReceiptRepository, times(1)).save(receipt);
    }

    @Test
    @DisplayName("입고 완료 처리 실패 - 이미 완료된 입고")
    void completeInbound_AlreadyCompleted() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setStatus(InboundReceipt.InboundStatus.COMPLETED);

        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));

        // when & then
        assertThatThrownBy(() -> inventoryService.completeInbound(10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INBOUND_STATUS_INVALID);
    }

    @Test
    @DisplayName("입고 완료 및 재고 적재 성공")
    void stackInventory_Success() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setItem(item);
        receipt.setPartner(partner);
        receipt.setLocation(location);
        receipt.setInboundQty(50);
        receipt.setStatus(InboundReceipt.InboundStatus.COMPLETED); // 적재는 COMPLETED 상태에서 가능

        InventoryStackRequest request = new InventoryStackRequest();
        request.setTargetLocationCode("LOC-A-01");

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));
        given(warehouseLocationRepository.findByLocationCode("LOC-A-01")).willReturn(Optional.of(location));
        given(currentInventoryRepository.findByItemAndLocation(item, location)).willReturn(Optional.empty());

        // when
        inventoryService.stackInventory(1, 10, request);

        // then
        verify(currentInventoryRepository, times(1)).save(any(CurrentInventory.class));
        verify(transactionHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("재고 적재 실패 - 입고 상태가 COMPLETED가 아님")
    void stackInventory_InvalidStatus() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setStatus(InboundReceipt.InboundStatus.READY);

        InventoryStackRequest request = new InventoryStackRequest();
        request.setTargetLocationCode("LOC-A-01");

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));

        // when & then
        assertThatThrownBy(() -> inventoryService.stackInventory(1, 10, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INBOUND_STATUS_INVALID);
    }

    @Test
    @DisplayName("재고 적재 실패 - 적재 용량 초과")
    void stackInventory_CapacityExceeded() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setItem(item);
        receipt.setInboundQty(600);
        receipt.setStatus(InboundReceipt.InboundStatus.COMPLETED);

        InventoryStackRequest request = new InventoryStackRequest();
        request.setTargetLocationCode("LOC-A-01");

        CurrentInventory existingInventory = new CurrentInventory();
        existingInventory.setCurrentQty(500); // 500 + 600 > 1000 (맥스용량)

        given(userRepository.findById(1)).willReturn(Optional.of(worker));
        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));
        given(warehouseLocationRepository.findByLocationCode("LOC-A-01")).willReturn(Optional.of(location));
        given(currentInventoryRepository.findByItemAndLocation(item, location)).willReturn(Optional.of(existingInventory));

        // when & then
        assertThatThrownBy(() -> inventoryService.stackInventory(1, 10, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOCATION_CAPACITY_EXCEEDED);
    }

    @Test
    @DisplayName("입고 정보 삭제 성공 - READY 상태")
    void deleteInbound_Success() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setStatus(InboundReceipt.InboundStatus.READY);

        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));

        // when
        inventoryService.deleteInbound(10);

        // then
        verify(inboundReceiptRepository, times(1)).delete(receipt);
    }

    @Test
    @DisplayName("입고 정보 삭제 실패 - COMPLETED 상태")
    void deleteInbound_FailedCompleted() {
        // given
        InboundReceipt receipt = new InboundReceipt();
        receipt.setInboundId(10);
        receipt.setStatus(InboundReceipt.InboundStatus.COMPLETED);

        given(inboundReceiptRepository.findById(10)).willReturn(Optional.of(receipt));

        // when & then
        assertThatThrownBy(() -> inventoryService.deleteInbound(10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INBOUND_CANNOT_DELETE);
    }

    @Test
    @DisplayName("로케이션 등록 성공 - 코드 중복이 없을 때")
    void createLocation_Success() {
        // given
        LocationRequest request = new LocationRequest("LOC-B-02", "서브창고", "2", "3");
        given(warehouseLocationRepository.existsByLocationCode("LOC-B-02")).willReturn(false);

        WarehouseLocation newLocation = new WarehouseLocation();
        newLocation.setLocationId(2);
        newLocation.setLocationCode("LOC-B-02");
        newLocation.setWarehouseName("서브창고");
        newLocation.setRackRow("2");
        newLocation.setRackColumn("3");

        given(warehouseLocationRepository.save(any(WarehouseLocation.class))).willReturn(newLocation);

        // when
        LocationResponse response = inventoryService.createLocation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getLocationCode()).isEqualTo("LOC-B-02");
        verify(warehouseLocationRepository, times(1)).save(any(WarehouseLocation.class));
    }

    @Test
    @DisplayName("로케이션 등록 실패 - 중복 코드 존재")
    void createLocation_DuplicateCode() {
        // given
        LocationRequest request = new LocationRequest("LOC-A-01", "메인창고", "1", "1");
        given(warehouseLocationRepository.existsByLocationCode("LOC-A-01")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> inventoryService.createLocation(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOCATION_CODE_DUPLICATE);
    }

    @Test
    @DisplayName("로케이션 삭제 실패 - 재고 보유 중")
    void deleteLocation_HasInventory() {
        // given
        given(warehouseLocationRepository.findById(1)).willReturn(Optional.of(location));
        given(currentInventoryRepository.existsByLocation(location)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> inventoryService.deleteLocation(1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOCATION_HAS_INVENTORY);
    }
}

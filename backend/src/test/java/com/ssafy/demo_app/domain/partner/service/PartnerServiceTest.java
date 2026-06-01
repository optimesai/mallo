package com.ssafy.demo_app.domain.partner.service;

import com.ssafy.demo_app.api.partner.dto.PartnerRequest;
import com.ssafy.demo_app.api.partner.dto.PartnerResponse;
import com.ssafy.demo_app.domain.inventory.repository.InboundReceiptRepository;
import com.ssafy.demo_app.domain.partner.entity.PartnerMaster;
import com.ssafy.demo_app.domain.partner.repository.PartnerMasterRepository;
import com.ssafy.demo_app.domain.shipping.repository.OutboundShippingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerMasterRepository partnerMasterRepository;

    @Mock
    private InboundReceiptRepository inboundReceiptRepository;

    @Mock
    private OutboundShippingRepository outboundShippingRepository;

    @InjectMocks
    private PartnerServiceImpl partnerService;

    private PartnerMaster supplier;
    private PartnerMaster customer;

    @BeforeEach
    void setUp() {
        supplier = new PartnerMaster();
        supplier.setPartnerId(1);
        supplier.setPartnerCode("SUP-POSCO-01");
        supplier.setPartnerName("(주)포스코 인터내셔널");
        supplier.setPartnerType(PartnerMaster.PartnerType.SUPPLIER);
        supplier.setBusinessNo("123-45-67890");
        supplier.setRepresentative("홍길동");
        supplier.setContactPhone("02-3457-1114");

        customer = new PartnerMaster();
        customer.setPartnerId(2);
        customer.setPartnerCode("CUS-HYUNDAI-M");
        customer.setPartnerName("현대모비스 울산공장");
        customer.setPartnerType(PartnerMaster.PartnerType.CUSTOMER);
        customer.setBusinessNo("113-81-22441");
        customer.setRepresentative("이영희");
        customer.setContactPhone("052-202-0114");
    }

    @Test
    @DisplayName("거래처 등록 성공")
    void createPartner_success() {
        PartnerRequest request = new PartnerRequest(
                " SUP-SAMSUNG-E ",
                " 삼성전자 디바이스솔루션 ",
                PartnerMaster.PartnerType.SUPPLIER,
                "220-81-62517",
                "김대표",
                "031-200-1114"
        );

        given(partnerMasterRepository.existsByPartnerCode(" SUP-SAMSUNG-E ")).willReturn(false);
        given(partnerMasterRepository.save(any(PartnerMaster.class))).willAnswer(invocation -> {
            PartnerMaster partner = invocation.getArgument(0);
            partner.setPartnerId(3);
            return partner;
        });

        PartnerResponse response = partnerService.createPartner(request);

        assertThat(response.getPartnerId()).isEqualTo(3);
        assertThat(response.getPartnerCode()).isEqualTo("SUP-SAMSUNG-E");
        assertThat(response.getPartnerName()).isEqualTo("삼성전자 디바이스솔루션");
        assertThat(response.getPartnerType()).isEqualTo(PartnerMaster.PartnerType.SUPPLIER);
    }

    @Test
    @DisplayName("거래처 등록 실패 - 거래처 코드 중복")
    void createPartner_duplicateCode() {
        PartnerRequest request = new PartnerRequest(
                "SUP-POSCO-01",
                "(주)포스코 인터내셔널",
                PartnerMaster.PartnerType.SUPPLIER,
                null,
                null,
                null
        );

        given(partnerMasterRepository.existsByPartnerCode("SUP-POSCO-01")).willReturn(true);

        assertThatThrownBy(() -> partnerService.createPartner(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNER_CODE_DUPLICATE);

        verify(partnerMasterRepository, never()).save(any(PartnerMaster.class));
    }

    @Test
    @DisplayName("거래처 목록 조회 성공")
    void getPartners_success() {
        given(partnerMasterRepository.findAll(any(Sort.class))).willReturn(List.of(supplier, customer));

        List<PartnerResponse> responses = partnerService.getPartners(null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(PartnerResponse::getPartnerCode)
                .containsExactly("SUP-POSCO-01", "CUS-HYUNDAI-M");
    }

    @Test
    @DisplayName("거래처 타입 필터 조회 성공")
    void getPartners_filterByType() {
        given(partnerMasterRepository.findByPartnerTypeOrderByPartnerIdAsc(PartnerMaster.PartnerType.SUPPLIER))
                .willReturn(List.of(supplier));

        List<PartnerResponse> responses = partnerService.getPartners(PartnerMaster.PartnerType.SUPPLIER, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPartnerType()).isEqualTo(PartnerMaster.PartnerType.SUPPLIER);
    }

    @Test
    @DisplayName("거래처 키워드 검색 성공")
    void getPartners_searchByKeyword() {
        given(partnerMasterRepository.findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrBusinessNoContainingIgnoreCaseOrderByPartnerIdAsc(
                "포스코",
                "포스코",
                "포스코"
        )).willReturn(List.of(supplier));

        List<PartnerResponse> responses = partnerService.getPartners(null, "포스코");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPartnerCode()).isEqualTo("SUP-POSCO-01");
    }

    @Test
    @DisplayName("거래처 검색 조회 성공 - ID")
    void searchPartners_byId_success() {
        given(partnerMasterRepository.findById(1)).willReturn(Optional.of(supplier));
        given(partnerMasterRepository.findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrderByPartnerIdAsc(
                "1",
                "1"
        )).willReturn(List.of());

        List<PartnerResponse> responses = partnerService.searchPartners("1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPartnerId()).isEqualTo(1);
        assertThat(responses.get(0).getPartnerCode()).isEqualTo("SUP-POSCO-01");
    }

    @Test
    @DisplayName("거래처 검색 조회 성공 - 코드")
    void searchPartners_byCode_success() {
        given(partnerMasterRepository.findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrderByPartnerIdAsc(
                "SUP-POSCO",
                "SUP-POSCO"
        )).willReturn(List.of(supplier));

        List<PartnerResponse> responses = partnerService.searchPartners("SUP-POSCO");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPartnerCode()).isEqualTo("SUP-POSCO-01");
    }

    @Test
    @DisplayName("거래처 검색 조회 성공 - 거래처명 다건")
    void searchPartners_byName_multipleResults() {
        PartnerMaster anotherCustomer = new PartnerMaster();
        anotherCustomer.setPartnerId(3);
        anotherCustomer.setPartnerCode("CUS-HYUNDAI-A");
        anotherCustomer.setPartnerName("현대모비스 아산공장");
        anotherCustomer.setPartnerType(PartnerMaster.PartnerType.CUSTOMER);

        given(partnerMasterRepository.findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrderByPartnerIdAsc(
                "현대모비스",
                "현대모비스"
        )).willReturn(List.of(customer, anotherCustomer));

        List<PartnerResponse> responses = partnerService.searchPartners("현대모비스");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(PartnerResponse::getPartnerName)
                .containsExactly("현대모비스 울산공장", "현대모비스 아산공장");
    }

    @Test
    @DisplayName("거래처 검색 조회 실패 - 거래처 없음")
    void searchPartners_notFound() {
        given(partnerMasterRepository.findById(999)).willReturn(Optional.empty());
        given(partnerMasterRepository.findByPartnerNameContainingIgnoreCaseOrPartnerCodeContainingIgnoreCaseOrderByPartnerIdAsc(
                "999",
                "999"
        )).willReturn(List.of());

        assertThatThrownBy(() -> partnerService.searchPartners("999"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNER_NOT_FOUND);
    }

    @Test
    @DisplayName("거래처 수정 성공")
    void updatePartner_success() {
        PartnerRequest request = new PartnerRequest(
                "SUP-POSCO-02",
                "포스코 공급사",
                PartnerMaster.PartnerType.SUPPLIER,
                "123-45-67890",
                "박대표",
                "02-0000-0000"
        );

        given(partnerMasterRepository.findById(1)).willReturn(Optional.of(supplier));
        given(partnerMasterRepository.existsByPartnerCodeAndPartnerIdNot("SUP-POSCO-02", 1)).willReturn(false);
        given(partnerMasterRepository.save(any(PartnerMaster.class))).willAnswer(invocation -> invocation.getArgument(0));

        PartnerResponse response = partnerService.updatePartner(1, request);

        assertThat(response.getPartnerCode()).isEqualTo("SUP-POSCO-02");
        assertThat(response.getPartnerName()).isEqualTo("포스코 공급사");
        assertThat(response.getRepresentative()).isEqualTo("박대표");
    }

    @Test
    @DisplayName("거래처 수정 실패 - 거래처 코드 중복")
    void updatePartner_duplicateCode() {
        PartnerRequest request = new PartnerRequest(
                "CUS-HYUNDAI-M",
                "포스코 공급사",
                PartnerMaster.PartnerType.SUPPLIER,
                null,
                null,
                null
        );

        given(partnerMasterRepository.findById(1)).willReturn(Optional.of(supplier));
        given(partnerMasterRepository.existsByPartnerCodeAndPartnerIdNot("CUS-HYUNDAI-M", 1)).willReturn(true);

        assertThatThrownBy(() -> partnerService.updatePartner(1, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNER_CODE_DUPLICATE);
    }

    @Test
    @DisplayName("거래처 삭제 성공 - 참조 이력 없음")
    void deletePartner_success() {
        given(partnerMasterRepository.findById(1)).willReturn(Optional.of(supplier));
        given(inboundReceiptRepository.existsByPartner(supplier)).willReturn(false);
        given(outboundShippingRepository.existsByPartner(supplier)).willReturn(false);

        partnerService.deletePartner(1);

        verify(partnerMasterRepository).delete(supplier);
    }

    @Test
    @DisplayName("거래처 삭제 실패 - 입고 이력에서 참조 중")
    void deletePartner_hasInboundReference() {
        given(partnerMasterRepository.findById(1)).willReturn(Optional.of(supplier));
        given(inboundReceiptRepository.existsByPartner(supplier)).willReturn(true);

        assertThatThrownBy(() -> partnerService.deletePartner(1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNER_HAS_REFERENCES);

        verify(partnerMasterRepository, never()).delete(any(PartnerMaster.class));
    }

    @Test
    @DisplayName("거래처 삭제 실패 - 출하 이력에서 참조 중")
    void deletePartner_hasShippingReference() {
        given(partnerMasterRepository.findById(2)).willReturn(Optional.of(customer));
        given(inboundReceiptRepository.existsByPartner(customer)).willReturn(false);
        given(outboundShippingRepository.existsByPartner(customer)).willReturn(true);

        assertThatThrownBy(() -> partnerService.deletePartner(2))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNER_HAS_REFERENCES);

        verify(partnerMasterRepository, never()).delete(any(PartnerMaster.class));
    }
}

package com.ssafy.demo_app.domain.routing.service;

import com.ssafy.demo_app.api.routing.dto.FactoryRoutingRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingStatusUpdateRequest;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingTreeResponse;
import com.ssafy.demo_app.api.routing.dto.FactoryRoutingUsageResponse;
import com.ssafy.demo_app.domain.production.entity.ProductionExecution;
import com.ssafy.demo_app.domain.production.entity.WorkOrder;
import com.ssafy.demo_app.domain.production.repository.ProductionExecutionRepository;
import com.ssafy.demo_app.domain.production.repository.WorkOrderRepository;
import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import com.ssafy.demo_app.domain.routing.repository.FactoryRoutingRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FactoryRoutingServiceTest {

    @Mock
    private FactoryRoutingRepository factoryRoutingRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProductionExecutionRepository productionExecutionRepository;

    @InjectMocks
    private FactoryRoutingServiceImpl factoryRoutingService;

    private FactoryRouting routing1;
    private FactoryRouting routing2;
    private FactoryRouting routing3;

    @BeforeEach
    void setUp() {
        routing1 = createRouting(1, "창원제1공장", "A라인", 1, "SMD 표면실장 공정");
        routing2 = createRouting(2, "창원제1공장", "A라인", 2, "프레임 기계조립 공정");
        routing3 = createRouting(3, "창원제1공장", "B라인", 1, "메인보드 배선 공정");
    }

    @Test
    @DisplayName("라우팅 등록 성공")
    void createRouting_success() {
        FactoryRoutingRequest request = new FactoryRoutingRequest(
                " 창원제1공장 ",
                " A라인 ",
                3,
                " 최종 검사 공정 "
        );

        given(factoryRoutingRepository.existsByFactoryNameAndLineNameAndOperationSeq(
                "창원제1공장",
                "A라인",
                3
        )).willReturn(false);
        given(factoryRoutingRepository.save(any(FactoryRouting.class))).willAnswer(invocation -> {
            FactoryRouting routing = invocation.getArgument(0);
            routing.setRoutingId(4);
            return routing;
        });

        FactoryRoutingResponse response = factoryRoutingService.createRouting(request);

        assertThat(response.getRoutingId()).isEqualTo(4);
        assertThat(response.getFactoryName()).isEqualTo("창원제1공장");
        assertThat(response.getLineName()).isEqualTo("A라인");
        assertThat(response.getOperationSeq()).isEqualTo(3);
        assertThat(response.getOperationName()).isEqualTo("최종 검사 공정");
    }

    @Test
    @DisplayName("라우팅 등록 실패 - 동일 공장/라인/공정 순서 중복")
    void createRouting_duplicate() {
        FactoryRoutingRequest request = new FactoryRoutingRequest(
                "창원제1공장",
                "A라인",
                1,
                "중복 공정"
        );

        given(factoryRoutingRepository.existsByFactoryNameAndLineNameAndOperationSeq(
                "창원제1공장",
                "A라인",
                1
        )).willReturn(true);

        assertThatThrownBy(() -> factoryRoutingService.createRouting(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTING_DUPLICATE);

        verify(factoryRoutingRepository, never()).save(any(FactoryRouting.class));
    }

    @Test
    @DisplayName("라우팅 등록 실패 - DB 유니크 제약 충돌")
    void createRouting_duplicateConstraint() {
        FactoryRoutingRequest request = new FactoryRoutingRequest(
                "창원제1공장",
                "A라인",
                1,
                "중복 공정"
        );

        given(factoryRoutingRepository.existsByFactoryNameAndLineNameAndOperationSeq(
                "창원제1공장",
                "A라인",
                1
        )).willReturn(false);
        given(factoryRoutingRepository.save(any(FactoryRouting.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> factoryRoutingService.createRouting(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTING_DUPLICATE);
    }

    @Test
    @DisplayName("라우팅 목록 조회 성공 - 공장/라인 필터")
    void getRoutings_filterByFactoryAndLine() {
        given(factoryRoutingRepository.findByFactoryNameAndLineNameOrderByOperationSeqAsc("창원제1공장", "A라인"))
                .willReturn(List.of(routing1, routing2));

        List<FactoryRoutingResponse> responses = factoryRoutingService.getRoutings("창원제1공장", "A라인", null);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(FactoryRoutingResponse::getOperationSeq)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("라우팅 수정 성공")
    void updateRouting_success() {
        FactoryRoutingRequest request = new FactoryRoutingRequest(
                "창원제1공장",
                "A라인",
                2,
                "수정된 공정"
        );

        given(factoryRoutingRepository.findById(1)).willReturn(Optional.of(routing1));
        given(workOrderRepository.existsByRouting(routing1)).willReturn(false);
        given(productionExecutionRepository.existsByRouting(routing1)).willReturn(false);
        given(factoryRoutingRepository.existsByFactoryNameAndLineNameAndOperationSeqAndRoutingIdNot(
                "창원제1공장",
                "A라인",
                2,
                1
        )).willReturn(false);
        given(factoryRoutingRepository.save(any(FactoryRouting.class))).willAnswer(invocation -> invocation.getArgument(0));

        FactoryRoutingResponse response = factoryRoutingService.updateRouting(1, request);

        assertThat(response.getOperationSeq()).isEqualTo(2);
        assertThat(response.getOperationName()).isEqualTo("수정된 공정");
    }

    @Test
    @DisplayName("라우팅 수정 실패 - 작업지시 참조 존재")
    void updateRouting_hasReference() {
        FactoryRoutingRequest request = new FactoryRoutingRequest(
                "창원제1공장",
                "A라인",
                2,
                "수정된 공정"
        );

        given(factoryRoutingRepository.findById(1)).willReturn(Optional.of(routing1));
        given(workOrderRepository.existsByRouting(routing1)).willReturn(true);

        assertThatThrownBy(() -> factoryRoutingService.updateRouting(1, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTING_HAS_REFERENCE);

        verify(factoryRoutingRepository, never()).save(any(FactoryRouting.class));
    }

    @Test
    @DisplayName("라우팅 삭제 실패 - 작업지시 참조 존재")
    void deleteRouting_hasWorkOrder() {
        given(factoryRoutingRepository.findById(1)).willReturn(Optional.of(routing1));
        given(workOrderRepository.existsByRouting(routing1)).willReturn(true);

        assertThatThrownBy(() -> factoryRoutingService.deleteRouting(1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTING_HAS_REFERENCE);

        verify(factoryRoutingRepository, never()).delete(any(FactoryRouting.class));
    }

    @Test
    @DisplayName("라우팅 삭제 실패 - 생산 실적 참조 존재")
    void deleteRouting_hasExecution() {
        given(factoryRoutingRepository.findById(1)).willReturn(Optional.of(routing1));
        given(workOrderRepository.existsByRouting(routing1)).willReturn(false);
        given(productionExecutionRepository.existsByRouting(routing1)).willReturn(true);

        assertThatThrownBy(() -> factoryRoutingService.deleteRouting(1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTING_HAS_REFERENCE);

        verify(factoryRoutingRepository, never()).delete(any(FactoryRouting.class));
    }

    @Test
    @DisplayName("라우팅 상태 변경 성공")
    void updateRoutingStatus_success() {
        FactoryRoutingStatusUpdateRequest request = new FactoryRoutingStatusUpdateRequest();
        request.setRoutingStatus(FactoryRouting.RoutingStatus.INACTIVE);

        given(factoryRoutingRepository.findById(1)).willReturn(Optional.of(routing1));
        given(factoryRoutingRepository.save(any(FactoryRouting.class))).willAnswer(invocation -> invocation.getArgument(0));

        FactoryRoutingResponse response = factoryRoutingService.updateRoutingStatus(1, request);

        assertThat(response.getRoutingStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("라우팅 참조 현황 조회 성공")
    void getRoutingUsage_success() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo("WO-20260615-001");
        ProductionExecution execution = new ProductionExecution();
        execution.setExecutionId(10);

        given(factoryRoutingRepository.findById(1)).willReturn(Optional.of(routing1));
        given(workOrderRepository.countByRouting(routing1)).willReturn(1L);
        given(productionExecutionRepository.countByRouting(routing1)).willReturn(1L);
        given(workOrderRepository.findTop5ByRoutingOrderByOrderIdDesc(routing1)).willReturn(List.of(workOrder));
        given(productionExecutionRepository.findTop5ByRoutingOrderByExecutionIdDesc(routing1)).willReturn(List.of(execution));

        FactoryRoutingUsageResponse response = factoryRoutingService.getRoutingUsage(1);

        assertThat(response.getWorkOrderCount()).isEqualTo(1);
        assertThat(response.getExecutionCount()).isEqualTo(1);
        assertThat(response.isCanDelete()).isFalse();
        assertThat(response.getWorkOrderNos()).containsExactly("WO-20260615-001");
        assertThat(response.getExecutionIds()).containsExactly(10);
    }

    @Test
    @DisplayName("라우팅 트리 조회 성공")
    void getRoutingTree_success() {
        given(factoryRoutingRepository.findAllByOrderByFactoryNameAscLineNameAscOperationSeqAsc())
                .willReturn(List.of(routing1, routing2, routing3));

        List<FactoryRoutingTreeResponse> responses = factoryRoutingService.getRoutingTree();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFactoryName()).isEqualTo("창원제1공장");
        assertThat(responses.get(0).getLines()).hasSize(2);
        assertThat(responses.get(0).getLines().get(0).getLineName()).isEqualTo("A라인");
        assertThat(responses.get(0).getLines().get(0).getOperations()).hasSize(2);
    }

    private FactoryRouting createRouting(
            Integer routingId,
            String factoryName,
            String lineName,
            Integer operationSeq,
            String operationName
    ) {
        FactoryRouting routing = new FactoryRouting();
        routing.setRoutingId(routingId);
        routing.setFactoryName(factoryName);
        routing.setLineName(lineName);
        routing.setOperationSeq(operationSeq);
        routing.setOperationName(operationName);
        return routing;
    }
}

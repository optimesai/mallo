package com.ssafy.demo_app.domain.ai.service.interpretation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuestionInterpretationServiceTest {

    private final AiQuestionInterpretationService service = new AiQuestionInterpretationService();

    @Test
    void interpret_extractsBomRequirementSlots() {
        AiQuestionInterpretation interpretation = service.interpret(
                "testparent 10개 만들려면 가장 최근 BOM 버전으로 어떤 품목이 몇 개 필요해?"
        );

        assertThat(interpretation.isDataQuestion()).isTrue();
        assertThat(interpretation.getDomain()).isEqualTo("bom");
        assertThat(interpretation.getIntent()).isEqualTo("material_requirement");
        assertThat(interpretation.getMetric()).isEqualTo("bom_requirement");
        assertThat(interpretation.getItemKeyword()).isEqualTo("testparent");
        assertThat(interpretation.getTargetQty()).isEqualTo(10);
        assertThat(interpretation.getBomVersionPolicy()).isEqualTo("LATEST");
        assertThat(interpretation.getSummary()).contains("도메인=bom");
        assertThat(interpretation.getSuggestedQuestions()).isNotEmpty();
    }

    @Test
    void interpret_extractsInventorySafetyStockQuestion() {
        AiQuestionInterpretation interpretation = service.interpret("안전재고 미만 품목을 보여줘");

        assertThat(interpretation.isDataQuestion()).isTrue();
        assertThat(interpretation.getDomain()).isEqualTo("inventory");
        assertThat(interpretation.getIntent()).isEqualTo("lookup");
        assertThat(interpretation.getMetric()).isEqualTo("safety_stock_shortage");
    }

    @Test
    void interpret_extractsShippingAggregateByPartner() {
        AiQuestionInterpretation interpretation = service.interpret("출하 대기 건수를 거래처별로 집계해줘");

        assertThat(interpretation.isDataQuestion()).isTrue();
        assertThat(interpretation.getDomain()).isEqualTo("shipping");
        assertThat(interpretation.getIntent()).isEqualTo("aggregate");
        assertThat(interpretation.getMetric()).isEqualTo("shipping_waiting_qty");
        assertThat(interpretation.getDimensions()).contains("partner");
    }

    @Test
    void interpret_extractsInboundTrendPeriod() {
        AiQuestionInterpretation interpretation = service.interpret("최근 7일 입고 수량 추이를 알려줘");

        assertThat(interpretation.isDataQuestion()).isTrue();
        assertThat(interpretation.getDomain()).isEqualTo("inbound");
        assertThat(interpretation.getIntent()).isEqualTo("trend");
        assertThat(interpretation.getMetric()).isEqualTo("inbound_qty");
        assertThat(interpretation.getTimeRange()).isEqualTo("recent_7_days");
    }

    @Test
    void interpret_extractsProductionDefectRateByLine() {
        AiQuestionInterpretation interpretation = service.interpret("라인별 불량률을 비교해줘");

        assertThat(interpretation.isDataQuestion()).isTrue();
        assertThat(interpretation.getDomain()).isEqualTo("production");
        assertThat(interpretation.getIntent()).isEqualTo("comparison");
        assertThat(interpretation.getMetric()).isEqualTo("defect_rate");
        assertThat(interpretation.getDimensions()).contains("line");
    }
}

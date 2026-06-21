package com.ssafy.demo_app.domain.ai.service.sql;

import com.ssafy.demo_app.domain.ai.service.classification.AiIntentResult;
import com.ssafy.demo_app.domain.ai.service.sql.SqlSemanticValidationService.SqlSemanticValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlSemanticValidationServiceTest {

    private final SqlSemanticValidationService sqlSemanticValidationService = new SqlSemanticValidationService();

    @Test
    void validate_blocksSafetyStockQueryWithoutSafetyStockComparison() {
        SqlSemanticValidationResult result = sqlSemanticValidationService.validate(
                "안전재고 미만 품목을 보여줘",
                AiIntentResult.dataQuestion(),
                "SELECT item_id FROM item_master"
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("안전재고");
    }

    @Test
    void validate_allowsSafetyStockQueryWithRequiredColumns() {
        SqlSemanticValidationResult result = sqlSemanticValidationService.validate(
                "안전재고 미만 품목을 보여줘",
                AiIntentResult.dataQuestion(),
                """
                        SELECT im.item_id, im.safety_stock, SUM(ci.current_qty) AS current_qty
                        FROM item_master im
                        LEFT JOIN current_inventory ci ON im.item_id = ci.item_id
                        GROUP BY im.item_id, im.safety_stock
                        HAVING SUM(ci.current_qty) < im.safety_stock
                        """
        );

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_blocksDefectRateWithoutFormulaParts() {
        SqlSemanticValidationResult result = sqlSemanticValidationService.validate(
                "라인별 불량률을 비교해줘",
                AiIntentResult.dataQuestion(),
                "SELECT fr.line_name, COUNT(*) AS defect_rate FROM production_execution pe JOIN factory_routing fr ON pe.routing_id = fr.routing_id GROUP BY fr.line_name"
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("불량률");
    }

    @Test
    void validate_blocksInventoryDomainWithoutInventoryTables() {
        AiIntentResult intentResult = AiIntentResult.dataQuestion();
        intentResult.setDomain("inventory");

        SqlSemanticValidationResult result = sqlSemanticValidationService.validate(
                "재고 현황을 알려줘",
                intentResult,
                "SELECT item_id, item_name FROM item_master"
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("재고");
    }

    @Test
    void validate_allowsInboundTrendWithInboundDate() {
        AiIntentResult intentResult = AiIntentResult.dataQuestion();
        intentResult.setDomain("inventory");

        SqlSemanticValidationResult result = sqlSemanticValidationService.validate(
                "최근 7일 입고 수량 추이를 알려줘",
                intentResult,
                """
                        SELECT
                          ir.inbound_date,
                          SUM(ir.inbound_qty) AS total_inbound_qty
                        FROM inbound_receipt ir
                        WHERE ir.inbound_date >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
                        GROUP BY ir.inbound_date
                        ORDER BY ir.inbound_date ASC
                        LIMIT 100
                        """
        );

        assertThat(result.isValid()).isTrue();
    }
}

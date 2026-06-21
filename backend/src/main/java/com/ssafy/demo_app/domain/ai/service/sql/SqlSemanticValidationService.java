package com.ssafy.demo_app.domain.ai.service.sql;

import com.ssafy.demo_app.domain.ai.service.classification.AiIntentResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SqlSemanticValidationService {

    public SqlSemanticValidationResult validate(String question, AiIntentResult intentResult, String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlSemanticValidationResult.invalid("SQL이 비어 있어 의미 검증을 수행할 수 없습니다.");
        }

        String normalizedQuestion = normalizeQuestion(question);
        String normalizedSql = normalizeSql(sql);

        SqlSemanticValidationResult result = validateQuestionRules(normalizedQuestion, normalizedSql);
        if (!result.isValid()) {
            return result;
        }

        return validateIntentRules(intentResult, normalizedSql);
    }

    private SqlSemanticValidationResult validateQuestionRules(String question, String sql) {
        if ((question.contains("안전재고") || question.contains("부족") || question.contains("미달"))
                && !(containsAll(sql, "item_master", "current_inventory", "safety_stock", "current_qty")
                && (sql.contains("having") || sql.contains("where")))) {
            return SqlSemanticValidationResult.invalid("안전재고/부족 질의에는 item_master.safety_stock과 current_inventory.current_qty 비교가 필요합니다.");
        }

        if ((question.contains("현재고") || question.contains("현재재고"))
                && !containsAll(sql, "current_inventory", "current_qty")) {
            return SqlSemanticValidationResult.invalid("현재고 질의에는 current_inventory.current_qty 사용이 필요합니다.");
        }

        if (question.contains("불량률")
                && !containsAll(sql, "defect_qty", "good_qty", "nullif")) {
            return SqlSemanticValidationResult.invalid("불량률 질의에는 defect_qty, good_qty, NULLIF 기반 계산식이 필요합니다.");
        }

        if ((question.contains("라인별") || question.contains("라인 별"))
                && !sql.contains("line_name")) {
            return SqlSemanticValidationResult.invalid("라인별 질의에는 factory_routing.line_name 기준 집계가 필요합니다.");
        }

        if ((question.contains("공정별") || question.contains("공정 별"))
                && !sql.contains("operation_name")) {
            return SqlSemanticValidationResult.invalid("공정별 질의에는 factory_routing.operation_name 기준 집계가 필요합니다.");
        }

        if ((question.contains("거래처별") || question.contains("거래처 별") || question.contains("고객사별") || question.contains("공급처별"))
                && !(sql.contains("partner_master") || sql.contains("partner_id"))) {
            return SqlSemanticValidationResult.invalid("거래처별 질의에는 partner_master 또는 partner_id 기준 집계가 필요합니다.");
        }

        if ((question.contains("창고별") || question.contains("창고 별"))
                && !sql.contains("warehouse_name")) {
            return SqlSemanticValidationResult.invalid("창고별 질의에는 warehouse_location.warehouse_name 기준 집계가 필요합니다.");
        }

        if ((question.contains("로케이션별") || question.contains("로케이션 별"))
                && !sql.contains("location_code")) {
            return SqlSemanticValidationResult.invalid("로케이션별 질의에는 warehouse_location.location_code 기준 집계가 필요합니다.");
        }

        if ((question.contains("추이") || question.contains("트렌드") || question.contains("일별") || question.contains("월별"))
                && !hasTimeBasis(sql)) {
            return SqlSemanticValidationResult.invalid("추이 질의에는 날짜 기준 집계 표현이 필요합니다.");
        }

        if ((question.contains("수불") || question.contains("입출고이력") || question.contains("입출고내역"))
                && !sql.contains("inventory_transaction_history")) {
            return SqlSemanticValidationResult.invalid("수불/입출고 이력 질의에는 inventory_transaction_history 사용이 필요합니다.");
        }

        if ((question.contains("출하대기") || question.contains("출고대기"))
                && !(sql.contains("outbound_shipping") && sql.contains("status"))) {
            return SqlSemanticValidationResult.invalid("출하 대기 질의에는 outbound_shipping.status 조건이 필요합니다.");
        }

        if (question.contains("bom") || question.contains("BOM") || question.contains("소요량")) {
            if (question.contains("소요량") && !containsAll(sql, "bom_structure", "quantity")) {
                return SqlSemanticValidationResult.invalid("BOM 소요량 질의에는 bom_structure.quantity 계산이 필요합니다.");
            }
        }

        return SqlSemanticValidationResult.valid();
    }

    private SqlSemanticValidationResult validateIntentRules(AiIntentResult intentResult, String sql) {
        if (intentResult == null) {
            return SqlSemanticValidationResult.valid();
        }

        String domain = normalizeSql(intentResult.getDomain());
        if ("inventory".equals(domain) && !(sql.contains("current_inventory")
                || sql.contains("inventory_transaction_history")
                || sql.contains("inbound_receipt"))) {
            return SqlSemanticValidationResult.invalid("재고 도메인 질의에는 재고 또는 수불 테이블 사용이 필요합니다.");
        }
        if ("shipping".equals(domain) && !sql.contains("outbound_shipping")) {
            return SqlSemanticValidationResult.invalid("출하 도메인 질의에는 outbound_shipping 사용이 필요합니다.");
        }
        if ("production".equals(domain) && !(sql.contains("work_order") || sql.contains("production_execution"))) {
            return SqlSemanticValidationResult.invalid("생산 도메인 질의에는 work_order 또는 production_execution 사용이 필요합니다.");
        }
        if ("quality".equals(domain) && !sql.contains("production_execution")) {
            return SqlSemanticValidationResult.invalid("품질 도메인 질의에는 production_execution 사용이 필요합니다.");
        }
        if ("bom".equals(domain) && !sql.contains("bom_structure")) {
            return SqlSemanticValidationResult.invalid("BOM 도메인 질의에는 bom_structure 사용이 필요합니다.");
        }

        return SqlSemanticValidationResult.valid();
    }

    private boolean containsAll(String sql, String... values) {
        for (String value : values) {
            if (!sql.contains(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasTimeBasis(String sql) {
        return sql.contains("date(")
                || sql.contains("year(")
                || sql.contains("month(")
                || sql.contains("date_format(")
                || sql.contains("created_at")
                || sql.contains("updated_at")
                || sql.contains("inbound_date")
                || sql.contains("plan_date")
                || sql.contains("estimated_delivery")
                || sql.contains("shipped_at")
                || sql.contains("transaction_date");
    }

    private String normalizeQuestion(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "");
    }

    private String normalizeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    @Getter
    @Setter
    public static class SqlSemanticValidationResult {

        private boolean valid;
        private String message;

        public static SqlSemanticValidationResult valid() {
            SqlSemanticValidationResult result = new SqlSemanticValidationResult();
            result.setValid(true);
            result.setMessage("success");
            return result;
        }

        public static SqlSemanticValidationResult invalid(String message) {
            SqlSemanticValidationResult result = new SqlSemanticValidationResult();
            result.setValid(false);
            result.setMessage(message);
            return result;
        }
    }
}

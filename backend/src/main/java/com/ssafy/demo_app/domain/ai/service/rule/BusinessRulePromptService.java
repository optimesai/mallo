package com.ssafy.demo_app.domain.ai.service.rule;

import org.springframework.stereotype.Service;

@Service
public class BusinessRulePromptService {

    private static final String BUSINESS_RULES = """
            Business Rules:
            - 현재고 means the sum of current_inventory.current_qty. Aggregate by item_id unless the user explicitly asks for warehouse, location, or lot detail.
            - 안전재고 미만 means SUM(current_inventory.current_qty) < item_master.safety_stock.
            - 안전재고 이하 means SUM(current_inventory.current_qty) <= item_master.safety_stock.
            - 수불 and 입출고 이력 mean inventory_transaction_history.
            - 입고 quantity means inbound_receipt.inbound_qty.
            - 입고 date means inbound_receipt.inbound_date unless the user asks by record creation time.
            - 출하 quantity means outbound_shipping.request_qty for requested quantity and outbound_shipping.shipped_qty for completed shipped quantity.
            - 출하 대기 means outbound_shipping.status in ('READY', 'PICKING', 'PACKING', 'INSPECTING', 'PARTIALLY_SHIPPED'). Exclude SHIPPED and CANCELED.
            - 작업지시 진행 means work_order.status in ('READY', 'RUN', 'HOLD'). CLOSE is completed.
            - 불량률 means SUM(production_execution.defect_qty) / NULLIF(SUM(production_execution.good_qty + production_execution.defect_qty), 0).
            - 생산량 means SUM(production_execution.good_qty + production_execution.defect_qty) unless the user explicitly asks for good quantity only.
            - 라인별 means GROUP BY factory_routing.line_name.
            - 공정별 means GROUP BY factory_routing.operation_name.
            - 품목별 means GROUP BY item_master.item_id, item_master.item_code, item_master.item_name.
            - 거래처별 means GROUP BY partner_master.partner_id, partner_master.partner_code, partner_master.partner_name.
            - 창고별 means GROUP BY warehouse_location.warehouse_name.
            - 로케이션별 means GROUP BY warehouse_location.location_code.
            - BOM 소요량 means bom_structure.quantity multiplied by the requested production quantity, using active bom_structure rows.
            - 최근 N일 means date_column >= DATE_SUB(CURRENT_DATE, INTERVAL N DAY).
            - 이번 달 means date_column >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') AND date_column < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH).
            - When choosing a date column, prefer the domain event date if present; otherwise use created_at.
            - Do not infer unknown enum values. Use only enum values listed in these rules or schema comments.
            """;

    public String getBusinessRules() {
        return BUSINESS_RULES;
    }
}

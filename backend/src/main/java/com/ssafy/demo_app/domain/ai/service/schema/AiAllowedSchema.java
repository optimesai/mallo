package com.ssafy.demo_app.domain.ai.service.schema;

import java.util.Set;

public final class AiAllowedSchema {

    public static final Set<String> ALLOWED_TABLES = Set.of(
            "item_master",
            "partner_master",
            "inbound_receipt",
            "current_inventory",
            "inventory_transaction_history",
            "warehouse_location",
            "outbound_shipping",
            "work_order",
            "production_execution",
            "factory_routing",
            "bom_structure"
    );

    private AiAllowedSchema() {
    }
}

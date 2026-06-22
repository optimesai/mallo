package com.ssafy.demo_app.domain.ai.service.schema;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchemaRelationshipProvider {

    public List<AiSchemaRelationship> getRelationships() {
        return List.of(
                AiSchemaRelationship.of("inbound_receipt", "item_id", "item_master", "item_id"),
                AiSchemaRelationship.of("inbound_receipt", "partner_id", "partner_master", "partner_id"),
                AiSchemaRelationship.of("inbound_receipt", "location_id", "warehouse_location", "location_id"),
                AiSchemaRelationship.of("current_inventory", "item_id", "item_master", "item_id"),
                AiSchemaRelationship.of("current_inventory", "location_id", "warehouse_location", "location_id"),
                AiSchemaRelationship.of("inventory_transaction_history", "item_id", "item_master", "item_id"),
                AiSchemaRelationship.of("inventory_transaction_history", "location_id", "warehouse_location", "location_id"),
                AiSchemaRelationship.of("inventory_transaction_history", "work_order_id", "work_order", "order_id"),
                AiSchemaRelationship.of("inventory_transaction_history", "production_execution_id", "production_execution", "execution_id"),
                AiSchemaRelationship.of("outbound_shipping", "item_id", "item_master", "item_id"),
                AiSchemaRelationship.of("outbound_shipping", "partner_id", "partner_master", "partner_id"),
                AiSchemaRelationship.of("outbound_shipping", "picking_location_id", "warehouse_location", "location_id"),
                AiSchemaRelationship.of("work_order", "item_id", "item_master", "item_id"),
                AiSchemaRelationship.of("work_order", "routing_id", "factory_routing", "routing_id"),
                AiSchemaRelationship.of("production_execution", "order_id", "work_order", "order_id"),
                AiSchemaRelationship.of("production_execution", "routing_id", "factory_routing", "routing_id"),
                AiSchemaRelationship.of("bom_structure", "parent_item_id", "item_master", "item_id"),
                AiSchemaRelationship.of("bom_structure", "child_item_id", "item_master", "item_id")
        );
    }
}

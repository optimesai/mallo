# AI SQL Schema Summary

- **Purpose**: AI SQL generation must use this schema summary as a static reference when reviewing the database shape.
- **Runtime Source**: `DatabaseSchemaService` reads allowed table metadata from `information_schema` and builds the live prompt schema.
- **Allowed Scope**: SQL generation must use only the tables and columns exposed by the runtime schema prompt.
- **SQL Type**: Generated SQL must be `SELECT` only.
- **Dialect**: MySQL 8.0.

## Runtime Prompt Flow

- `AiQueryServiceImpl` loads the schema before intent classification and SQL generation.
- `DatabaseSchemaMetadataReader` reads allowed tables from `information_schema`.
- `SchemaRelationshipProvider` supplies explicit join relationships.
- `SchemaPromptBuilder` converts table metadata and relationships into the schema prompt.
- `SqlAssistant` receives `Database Schema`, `Business Rules`, `Classification Result`, and few-shot examples before generating SQL.
- `SqlValidationService`, `SqlSemanticValidationService`, and `SqlReviewService` validate generated SQL before execution.

## Allowed Tables

- `item_master`: item master data.
- `partner_master`: partner master data.
- `inbound_receipt`: inbound receipt facts.
- `current_inventory`: current inventory facts.
- `inventory_transaction_history`: inventory transaction facts.
- `warehouse_location`: warehouse and location master data.
- `outbound_shipping`: outbound shipping facts.
- `work_order`: production work order data.
- `production_execution`: production execution facts.
- `factory_routing`: factory, line, and operation routing master data.
- `bom_structure`: bill of materials relationships.

## Relationships

- `inbound_receipt.item_id` -> `item_master.item_id`
- `inbound_receipt.partner_id` -> `partner_master.partner_id`
- `inbound_receipt.location_id` -> `warehouse_location.location_id`
- `current_inventory.item_id` -> `item_master.item_id`
- `current_inventory.location_id` -> `warehouse_location.location_id`
- `inventory_transaction_history.item_id` -> `item_master.item_id`
- `inventory_transaction_history.location_id` -> `warehouse_location.location_id`
- `inventory_transaction_history.work_order_id` -> `work_order.order_id`
- `inventory_transaction_history.production_execution_id` -> `production_execution.execution_id`
- `outbound_shipping.item_id` -> `item_master.item_id`
- `outbound_shipping.partner_id` -> `partner_master.partner_id`
- `outbound_shipping.picking_location_id` -> `warehouse_location.location_id`
- `work_order.item_id` -> `item_master.item_id`
- `work_order.routing_id` -> `factory_routing.routing_id`
- `production_execution.order_id` -> `work_order.order_id`
- `production_execution.routing_id` -> `factory_routing.routing_id`
- `bom_structure.parent_item_id` -> `item_master.item_id`
- `bom_structure.child_item_id` -> `item_master.item_id`

## Preferred Date Columns

- `inbound_receipt`: `inbound_date`
- `outbound_shipping`: `shipped_at` or `created_at`
- `inventory_transaction_history`: `created_at`
- `production_execution`: `created_at`
- `work_order`: `created_at`
- `ai_query_history`: `created_at`

## Business Meaning Rules

- **현재고**: Sum `current_inventory.current_qty` by `item_id` unless warehouse, location, or lot detail is requested.
- **안전재고 부족 수량**: `item_master.safety_stock - SUM(current_inventory.current_qty)`.
- **수불 / 입출고 이력**: Use `inventory_transaction_history`.
- **입고 수량**: Use `inbound_receipt.inbound_qty`.
- **출하 대기 잔량**: `outbound_shipping.request_qty - COALESCE(outbound_shipping.shipped_qty, 0)`.
- **불량률**: `SUM(production_execution.defect_qty) / NULLIF(SUM(production_execution.good_qty + production_execution.defect_qty), 0)`.
- **생산량**: `SUM(production_execution.good_qty + production_execution.defect_qty)` unless good quantity only is requested.
- **BOM 소요량**: `bom_structure.quantity` multiplied by the requested production target quantity.
- **BOM 최신 버전**: Latest active `bom_structure.bom_version` for the matched parent item.

## Entity Matching Rules

- Item keywords must be matched against `item_master.item_code`, `item_master.item_name`, and `item_master.item_id`.
- Partner keywords must be matched against `partner_master.partner_code`, `partner_master.partner_name`, and `partner_master.partner_id`.
- BOM production target items must match `bom_structure.parent_item_id`.
- BOM material items must match `bom_structure.child_item_id`.
- Free-form item values must not be assumed to be item codes only.

## Status Rules

- **출하 대기**: `outbound_shipping.status IN ('READY', 'PICKING', 'PACKING', 'INSPECTING', 'PARTIALLY_SHIPPED')`.
- **작업지시 진행**: `work_order.status IN ('READY', 'RUN', 'HOLD')`.
- **보류 작업지시**: `work_order.status = 'HOLD'`.
- **완료 작업지시**: `work_order.status = 'CLOSE'`.

## SQL Generation Guardrails

- Use explicit joins from the relationship list when possible.
- Do not invent tables, columns, or enum values.
- Add `LIMIT 100` unless the query returns a single aggregate row.
- Use deterministic secondary ordering for ranking queries.
- Use `NULLIF` for rate denominators.
- Return decimal ratios between `0` and `1` for rates unless percent is explicitly requested.
- Include human-readable labels where possible instead of exposing numeric IDs only.

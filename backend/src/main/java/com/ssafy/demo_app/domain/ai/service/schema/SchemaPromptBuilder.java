package com.ssafy.demo_app.domain.ai.service.schema;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SchemaPromptBuilder {

    public String build(List<AiSchemaTable> tables, List<AiSchemaRelationship> relationships) {
        StringBuilder builder = new StringBuilder();
        builder.append("Tables:\n");

        tables.stream()
                .sorted(Comparator.comparing(AiSchemaTable::getTableName))
                .forEach(table -> appendTable(builder, table));

        builder.append("\nRelationships:\n");
        relationships.forEach(relationship -> builder
                .append("- ")
                .append(relationship.getSourceTable())
                .append(".")
                .append(relationship.getSourceColumn())
                .append(" -> ")
                .append(relationship.getTargetTable())
                .append(".")
                .append(relationship.getTargetColumn())
                .append("\n"));

        return builder.toString().trim();
    }

    private void appendTable(StringBuilder builder, AiSchemaTable table) {
        builder.append("- ")
                .append(table.getTableName())
                .append(": ")
                .append(resolveDescription(table.getTableComment()))
                .append("\n")
                .append("  Role: ")
                .append(resolveTableRole(table.getTableName()))
                .append("\n")
                .append("  Preferred date column: ")
                .append(resolvePreferredDateColumn(table.getTableName()))
                .append("\n")
                .append("  Columns:\n");

        table.getColumns().forEach(column -> builder
                .append("  - ")
                .append(column.getColumnName())
                .append(" ")
                .append(resolveColumnType(column))
                .append(resolveColumnKey(column))
                .append(column.isNullable() ? " NULL" : " NOT NULL")
                .append(": ")
                .append(resolveDescription(column.getColumnComment()))
                .append("\n"));
    }

    private String resolveColumnKey(AiSchemaColumn column) {
        if (column.getColumnKey() == null || column.getColumnKey().isBlank()) {
            return "";
        }
        return " " + column.getColumnKey();
    }

    private String resolveDescription(String description) {
        if (description == null || description.isBlank()) {
            return "설명 없음";
        }
        return description;
    }

    private String resolveColumnType(AiSchemaColumn column) {
        if (column.getColumnType() != null && !column.getColumnType().isBlank()) {
            return column.getColumnType().toUpperCase();
        }
        return column.getDataType().toUpperCase();
    }

    private String resolveTableRole(String tableName) {
        return switch (tableName) {
            case "inventory_transaction_history",
                 "production_execution",
                 "inbound_receipt",
                 "outbound_shipping" -> "FACT_TABLE";

            case "item_master",
                 "partner_master",
                 "warehouse_location",
                 "factory_routing",
                 "bom_structure" -> "MASTER_TABLE";

            case "work_order" -> "ORDER_TABLE";

            case "ai_query_history" -> "HISTORY_TABLE";

            default -> "UNKNOWN";
        };
    }

    private String resolvePreferredDateColumn(String tableName) {
        return switch (tableName) {
            case "inbound_receipt" -> "inbound_date";
            case "outbound_shipping" -> "shipped_at or created_at";
            case "inventory_transaction_history" -> "created_at";
            case "production_execution" -> "created_at";
            case "work_order" -> "created_at";
            case "ai_query_history" -> "created_at";
            default -> "created_at if exists";
        };
    }

}

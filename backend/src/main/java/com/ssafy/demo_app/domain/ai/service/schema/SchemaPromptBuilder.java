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
                .append("  Columns:\n");

        table.getColumns().forEach(column -> builder
                .append("  - ")
                .append(column.getColumnName())
                .append(" ")
                .append(column.getDataType().toUpperCase())
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
}

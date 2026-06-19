package com.ssafy.demo_app.domain.ai.service.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaPromptBuilderTest {

    private final SchemaPromptBuilder schemaPromptBuilder = new SchemaPromptBuilder();

    @Test
    void build_includesTablesColumnsAndRelationships() {
        AiSchemaColumn column = new AiSchemaColumn();
        column.setColumnName("routing_id");
        column.setColumnComment("라우팅 ID");
        column.setDataType("bigint");
        column.setColumnKey("MUL");
        column.setNullable(false);

        AiSchemaTable table = new AiSchemaTable();
        table.setTableName("production_execution");
        table.setTableComment("생산 실적");
        table.getColumns().add(column);

        AiSchemaRelationship relationship = AiSchemaRelationship.of(
                "production_execution",
                "routing_id",
                "factory_routing",
                "routing_id"
        );

        String prompt = schemaPromptBuilder.build(List.of(table), List.of(relationship));

        assertThat(prompt).contains("production_execution: 생산 실적");
        assertThat(prompt).contains("routing_id BIGINT MUL NOT NULL: 라우팅 ID");
        assertThat(prompt).contains("production_execution.routing_id -> factory_routing.routing_id");
    }

    @Test
    void build_usesFallbackDescription() {
        AiSchemaColumn column = new AiSchemaColumn();
        column.setColumnName("item_id");
        column.setDataType("int");
        column.setNullable(true);

        AiSchemaTable table = new AiSchemaTable();
        table.setTableName("item_master");
        table.getColumns().add(column);

        String prompt = schemaPromptBuilder.build(List.of(table), List.of());

        assertThat(prompt).contains("item_master: 설명 없음");
        assertThat(prompt).contains("item_id INT NULL: 설명 없음");
    }
}

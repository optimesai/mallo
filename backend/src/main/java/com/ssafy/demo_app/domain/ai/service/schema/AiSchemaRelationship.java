package com.ssafy.demo_app.domain.ai.service.schema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSchemaRelationship {

    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;

    public static AiSchemaRelationship of(
            String sourceTable,
            String sourceColumn,
            String targetTable,
            String targetColumn
    ) {
        AiSchemaRelationship relationship = new AiSchemaRelationship();
        relationship.setSourceTable(sourceTable);
        relationship.setSourceColumn(sourceColumn);
        relationship.setTargetTable(targetTable);
        relationship.setTargetColumn(targetColumn);
        return relationship;
    }
}

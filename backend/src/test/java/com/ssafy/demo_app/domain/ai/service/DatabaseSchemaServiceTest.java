package com.ssafy.demo_app.domain.ai.service;

import com.ssafy.demo_app.domain.ai.service.schema.AiSchemaRelationship;
import com.ssafy.demo_app.domain.ai.service.schema.AiSchemaTable;
import com.ssafy.demo_app.domain.ai.service.schema.DatabaseSchemaMetadataReader;
import com.ssafy.demo_app.domain.ai.service.schema.SchemaPromptBuilder;
import com.ssafy.demo_app.domain.ai.service.schema.SchemaRelationshipProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DatabaseSchemaServiceTest {

    @Test
    void getSchemaDescription_reusesCachedSchema() {
        DatabaseSchemaMetadataReader metadataReader = mock(DatabaseSchemaMetadataReader.class);
        SchemaRelationshipProvider relationshipProvider = mock(SchemaRelationshipProvider.class);
        SchemaPromptBuilder promptBuilder = mock(SchemaPromptBuilder.class);
        DatabaseSchemaService databaseSchemaService = new DatabaseSchemaService(
                metadataReader,
                relationshipProvider,
                promptBuilder
        );

        List<AiSchemaTable> tables = List.of(new AiSchemaTable());
        List<AiSchemaRelationship> relationships = List.of();
        given(metadataReader.readAllowedTables()).willReturn(tables);
        given(relationshipProvider.getRelationships()).willReturn(relationships);
        given(promptBuilder.build(tables, relationships)).willReturn("schema prompt");

        String first = databaseSchemaService.getSchemaDescription();
        String second = databaseSchemaService.getSchemaDescription();

        assertThat(first).isEqualTo("schema prompt");
        assertThat(second).isEqualTo("schema prompt");
        verify(metadataReader, times(1)).readAllowedTables();
    }

    @Test
    void evictCache_reloadsSchema() {
        DatabaseSchemaMetadataReader metadataReader = mock(DatabaseSchemaMetadataReader.class);
        SchemaRelationshipProvider relationshipProvider = mock(SchemaRelationshipProvider.class);
        SchemaPromptBuilder promptBuilder = mock(SchemaPromptBuilder.class);
        DatabaseSchemaService databaseSchemaService = new DatabaseSchemaService(
                metadataReader,
                relationshipProvider,
                promptBuilder
        );

        List<AiSchemaTable> tables = List.of(new AiSchemaTable());
        List<AiSchemaRelationship> relationships = List.of();
        given(metadataReader.readAllowedTables()).willReturn(tables);
        given(relationshipProvider.getRelationships()).willReturn(relationships);
        given(promptBuilder.build(tables, relationships)).willReturn("schema prompt");

        databaseSchemaService.getSchemaDescription();
        databaseSchemaService.evictCache();
        databaseSchemaService.getSchemaDescription();

        verify(metadataReader, times(2)).readAllowedTables();
    }
}

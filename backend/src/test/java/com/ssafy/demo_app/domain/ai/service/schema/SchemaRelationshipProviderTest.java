package com.ssafy.demo_app.domain.ai.service.schema;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaRelationshipProviderTest {

    private final SchemaRelationshipProvider schemaRelationshipProvider = new SchemaRelationshipProvider();

    @Test
    void getRelationships_includesCoreJoinRelationships() {
        assertThat(schemaRelationshipProvider.getRelationships())
                .anySatisfy(relationship -> {
                    assertThat(relationship.getSourceTable()).isEqualTo("production_execution");
                    assertThat(relationship.getSourceColumn()).isEqualTo("routing_id");
                    assertThat(relationship.getTargetTable()).isEqualTo("factory_routing");
                    assertThat(relationship.getTargetColumn()).isEqualTo("routing_id");
                })
                .anySatisfy(relationship -> {
                    assertThat(relationship.getSourceTable()).isEqualTo("bom_structure");
                    assertThat(relationship.getSourceColumn()).isEqualTo("child_item_id");
                    assertThat(relationship.getTargetTable()).isEqualTo("item_master");
                    assertThat(relationship.getTargetColumn()).isEqualTo("item_id");
                });
    }
}

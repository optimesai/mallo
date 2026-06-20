package com.ssafy.demo_app.domain.ai.service.schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final DatabaseSchemaMetadataReader databaseSchemaMetadataReader;
    private final SchemaRelationshipProvider schemaRelationshipProvider;
    private final SchemaPromptBuilder schemaPromptBuilder;

    private String cachedSchemaDescription;
    private Instant cachedAt;

    public synchronized String getSchemaDescription() {
        if (cachedSchemaDescription != null && !isExpired()) {
            return cachedSchemaDescription;
        }

        cachedSchemaDescription = reloadSchemaDescription();
        cachedAt = Instant.now();
        return cachedSchemaDescription;
    }

    public synchronized void evictCache() {
        cachedSchemaDescription = null;
        cachedAt = null;
    }

    private boolean isExpired() {
        return cachedAt == null || cachedAt.plus(CACHE_TTL).isBefore(Instant.now());
    }

    private String reloadSchemaDescription() {
        List<AiSchemaTable> tables = databaseSchemaMetadataReader.readAllowedTables();
        List<AiSchemaRelationship> relationships = schemaRelationshipProvider.getRelationships();
        return schemaPromptBuilder.build(tables, relationships);
    }
}

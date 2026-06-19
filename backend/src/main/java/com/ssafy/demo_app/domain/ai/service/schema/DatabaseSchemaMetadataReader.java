package com.ssafy.demo_app.domain.ai.service.schema;

import com.ssafy.demo_app.domain.ai.service.AiAllowedSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatabaseSchemaMetadataReader {

    private final JdbcTemplate jdbcTemplate;

    public List<AiSchemaTable> readAllowedTables() {
        List<String> allowedTables = AiAllowedSchema.ALLOWED_TABLES.stream().toList();
        String placeholders = String.join(",", allowedTables.stream()
                .map(table -> "?")
                .toList());
        String sql = """
                SELECT
                  t.table_name,
                  t.table_comment,
                  c.column_name,
                  c.column_comment,
                  c.data_type,
                  c.column_key,
                  c.is_nullable,
                  c.ordinal_position
                FROM information_schema.tables t
                JOIN information_schema.columns c
                  ON t.table_schema = c.table_schema
                 AND t.table_name = c.table_name
                WHERE t.table_schema = DATABASE()
                  AND t.table_name IN (%s)
                ORDER BY t.table_name, c.ordinal_position
                """.formatted(placeholders);

        Map<String, AiSchemaTable> tables = new LinkedHashMap<>();
        jdbcTemplate.query(sql, ps -> {
            for (int index = 0; index < allowedTables.size(); index++) {
                ps.setString(index + 1, allowedTables.get(index));
            }
        }, rs -> {
            String tableName = rs.getString("table_name");
            AiSchemaTable table = tables.computeIfAbsent(tableName, ignored -> {
                AiSchemaTable created = new AiSchemaTable();
                created.setTableName(tableName);
                return created;
            });
            table.setTableComment(rs.getString("table_comment"));

            AiSchemaColumn column = new AiSchemaColumn();
            column.setColumnName(rs.getString("column_name"));
            column.setColumnComment(rs.getString("column_comment"));
            column.setDataType(rs.getString("data_type"));
            column.setColumnKey(rs.getString("column_key"));
            column.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
            table.getColumns().add(column);
        });

        return new ArrayList<>(tables.values());
    }
}

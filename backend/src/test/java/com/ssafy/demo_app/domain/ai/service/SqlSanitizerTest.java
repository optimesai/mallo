package com.ssafy.demo_app.domain.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlSanitizerTest {

    private final SqlSanitizer sqlSanitizer = new SqlSanitizer();

    @Test
    void sanitize_removesMarkdownAndTrailingSemicolon() {
        String sql = """
                ```sql
                SELECT * FROM item_master;
                ```
                """;

        String sanitized = sqlSanitizer.sanitize(sql);

        assertThat(sanitized).isEqualTo("SELECT * FROM item_master");
    }
}

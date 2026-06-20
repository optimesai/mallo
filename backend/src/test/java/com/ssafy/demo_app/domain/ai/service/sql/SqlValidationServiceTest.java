package com.ssafy.demo_app.domain.ai.service.sql;

import com.ssafy.demo_app.domain.ai.service.sql.SqlValidationService.SqlValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlValidationServiceTest {

    private final SqlValidationService sqlValidationService = new SqlValidationService();

    @Test
    void validate_allowsSelectFromAllowedTable() {
        SqlValidationResult result = sqlValidationService.validate("SELECT item_id, item_name FROM item_master");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNormalizedSql()).endsWith("LIMIT 100");
    }

    @Test
    void validate_blocksDelete() {
        SqlValidationResult result = sqlValidationService.validate("DELETE FROM item_master");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validate_blocksMultiStatement() {
        SqlValidationResult result = sqlValidationService.validate("SELECT * FROM item_master; SELECT * FROM user");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validate_blocksComments() {
        SqlValidationResult result = sqlValidationService.validate("SELECT * FROM item_master -- comment");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validate_blocksUnknownTable() {
        SqlValidationResult result = sqlValidationService.validate("SELECT * FROM user");

        assertThat(result.isValid()).isFalse();
    }
}

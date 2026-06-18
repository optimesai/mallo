package com.ssafy.demo_app.domain.ai.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlValidationResult {

    private boolean valid;
    private String message;
    private String normalizedSql;

    public static SqlValidationResult valid(String normalizedSql) {
        SqlValidationResult result = new SqlValidationResult();
        result.setValid(true);
        result.setMessage("success");
        result.setNormalizedSql(normalizedSql);
        return result;
    }

    public static SqlValidationResult invalid(String message) {
        SqlValidationResult result = new SqlValidationResult();
        result.setValid(false);
        result.setMessage(message);
        return result;
    }
}

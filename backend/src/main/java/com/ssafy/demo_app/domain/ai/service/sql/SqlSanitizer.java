package com.ssafy.demo_app.domain.ai.service.sql;

import org.springframework.stereotype.Service;

@Service
public class SqlSanitizer {

    public String sanitize(String sql) {
        if (sql == null) {
            return "";
        }

        String sanitized = sql
                .replace("```sql", "")
                .replace("```", "")
                .trim();

        if (sanitized.endsWith(";")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }

        return sanitized;
    }
}

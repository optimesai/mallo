package com.ssafy.demo_app.domain.ai.service.sql;

import com.ssafy.demo_app.domain.ai.service.schema.AiAllowedSchema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlValidationService {

    private static final int DEFAULT_LIMIT = 100;
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\b(from|join)\\s+([`\\w.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\blimit\\s+\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT",
            "UPDATE",
            "DELETE",
            "DROP",
            "ALTER",
            "TRUNCATE",
            "CREATE",
            "REPLACE",
            "MERGE",
            "CALL",
            "EXEC",
            "GRANT",
            "REVOKE",
            "SET",
            "USE",
            "LOCK",
            "UNLOCK"
    );
    public SqlValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlValidationResult.invalid("SQL이 비어 있습니다.");
        }

        String normalizedSql = sql.trim();
        String upperSql = normalizedSql.toUpperCase(Locale.ROOT);

        if (!upperSql.startsWith("SELECT")) {
            return SqlValidationResult.invalid("SELECT 문만 실행할 수 있습니다.");
        }
        if (hasBlockedCommentOrStatement(normalizedSql)) {
            return SqlValidationResult.invalid("주석 또는 다중 쿼리는 실행할 수 없습니다.");
        }
        if (containsForbiddenKeyword(upperSql)) {
            return SqlValidationResult.invalid("데이터 변경 또는 관리 명령은 실행할 수 없습니다.");
        }
        if (!usesOnlyAllowedTables(normalizedSql)) {
            return SqlValidationResult.invalid("허용되지 않은 테이블을 조회할 수 없습니다.");
        }

        return SqlValidationResult.valid(applyDefaultLimit(normalizedSql));
    }

    private boolean hasBlockedCommentOrStatement(String sql) {
        return sql.contains(";")
                || sql.contains("--")
                || sql.contains("/*")
                || sql.contains("*/")
                || sql.contains("#");
    }

    private boolean containsForbiddenKeyword(String upperSql) {
        String paddedSql = " " + upperSql.replaceAll("\\s+", " ") + " ";
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (paddedSql.contains(" " + keyword + " ")) {
                return true;
            }
        }
        return false;
    }

    private boolean usesOnlyAllowedTables(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        boolean foundTable = false;

        while (matcher.find()) {
            foundTable = true;
            String tableName = normalizeTableName(matcher.group(2));
            if (!AiAllowedSchema.ALLOWED_TABLES.contains(tableName)) {
                return false;
            }
        }

        return foundTable;
    }

    private String normalizeTableName(String tableName) {
        String normalized = tableName.replace("`", "").toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf(".");
        if (dotIndex >= 0) {
            return normalized.substring(dotIndex + 1);
        }
        return normalized;
    }

    private String applyDefaultLimit(String sql) {
        if (LIMIT_PATTERN.matcher(sql).find() || isAggregateSingleRowQuery(sql)) {
            return sql;
        }
        return sql + " LIMIT " + DEFAULT_LIMIT;
    }

    private boolean isAggregateSingleRowQuery(String sql) {
        String upperSql = sql.toUpperCase(Locale.ROOT);
        return !upperSql.contains(" GROUP BY ")
                && (upperSql.contains("COUNT(")
                || upperSql.contains("SUM(")
                || upperSql.contains("AVG(")
                || upperSql.contains("MIN(")
                || upperSql.contains("MAX("));
    }

    @Getter
    @Setter
    public static class SqlValidationResult {

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
}

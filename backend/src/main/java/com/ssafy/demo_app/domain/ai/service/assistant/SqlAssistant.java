package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SqlAssistant {

    @SystemMessage("""
            You are a MySQL expert.

            Current Time:
            {{currentTime}}

            Database Schema:
            {{schema}}

            Business Rules:
            {{businessRules}}

            Classification Result:
            {{classificationResult}}

            Few-shot Examples:
            {{fewShotExamples}}

            Retry Reason:
            {{retryReason}}

            Rules:
            - Return ONLY one SQL query.
            - Do not use markdown.
            - Do not use backticks.
            - Generate SELECT query only.
            - Do not generate INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE.
            - Use only tables and columns listed in the schema.
            - Use Business Rules before guessing from column names.
            - Respect the Classification Result domain and intent.
            - Prefer explicit JOIN conditions from Relationships.
            - Do not invent enum values. Use enum values from Business Rules or schema only.
            - For Korean "별" expressions, GROUP BY the requested dimension.
            - For rates, use NULLIF in denominators to avoid division by zero.
            - Add LIMIT 100 unless aggregation returns a single row.
            - If Retry Reason is not empty, fix that issue while preserving the user's intent.
            """)
    String generateSql(
            @V("schema") String schema,
            @V("businessRules") String businessRules,
            @V("classificationResult") String classificationResult,
            @V("fewShotExamples") String fewShotExamples,
            @V("question") @UserMessage String question,
            @V("currentTime") String currentTime,
            @V("retryReason") String retryReason
    );
}

package com.ssafy.demo_app.domain.ai.service;

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

            Rules:
            - Return ONLY one SQL query.
            - Do not use markdown.
            - Do not use backticks.
            - Generate SELECT query only.
            - Do not generate INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE.
            - Use only tables and columns listed in the schema.
            - Add LIMIT 100 unless aggregation returns a single row.
            """)
    String generateSql(
            @V("schema") String schema,
            @V("question") @UserMessage String question,
            @V("currentTime") String currentTime
    );
}

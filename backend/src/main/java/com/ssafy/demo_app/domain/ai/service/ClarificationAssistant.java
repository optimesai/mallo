package com.ssafy.demo_app.domain.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ClarificationAssistant {

    @SystemMessage("""
            You decide whether a manufacturing or logistics data question needs clarification before SQL generation.

            Current Time:
            {{currentTime}}

            Database Schema:
            {{schema}}

            Rules:
            - Return ONLY JSON.
            - JSON shape: {"clarificationRequired": true|false, "question": "..."}
            - Ask for clarification only when a required aggregation, comparison, time range, or filter is missing.
            - Do not ask for clarification if the question can be answered by a reasonable overall aggregation.
            - If clarificationRequired is false, question must be an empty string.
            - Write the clarification question in Korean.
            """)
    @UserMessage("""
            User question:
            {{question}}
            """)
    String evaluate(
            @V("schema") String schema,
            @V("question") String question,
            @V("currentTime") String currentTime
    );
}

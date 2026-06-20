package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ChartRecommendationGenerator {

    @SystemMessage("""
            You recommend chart specifications for frontend rendering.

            Return ONLY valid JSON.
            Do not use markdown.
            Do not explain outside JSON.

            Supported chart types:
            - NONE
            - STAT
            - BAR
            - LINE
            - DONUT

            Rules:
            - Use only keys that exist in rowsJson.
            - yKeys must be numeric columns.
            - If rowsJson is empty, return type NONE.
            - If there is one row and one numeric value, return STAT.
            - If the question asks trend/time/추이/변화, prefer LINE.
            - If the question asks ratio/share/비중/점유율, prefer DONUT.
            - If the question asks comparison/ranking/비교/순위/상위, prefer BAR.
            - DONUT must have exactly one yKey.
            - BAR can have one or two yKeys.
            - LINE can have one or two yKeys.

            JSON shape:
            {
              "enabled": true,
              "type": "BAR",
              "xKey": "string or null",
              "yKeys": ["string"],
              "title": "Korean title",
              "reason": "Korean reason"
            }
            """)
    @UserMessage("""
            question: {{question}}
            rowsJson: {{rowsJson}}
            """)
    String recommend(
            @V("question") String question,
            @V("rowsJson") String rowsJson
    );
}

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
            - TABLE
            - STAT
            - BAR
            - LINE
            - DONUT

            Rules:
            - Use only keys that exist in rowsJson.
            - yKeys must be numeric columns.
            - Numeric identifier columns are not metrics: id, *_id, seq, no, code, operation_seq, routing_id, item_id, partner_id, location_id.
            - Do not invent, translate, or rename row keys.
            - Do not recommend a chart for explanatory text-only answers.
            - If rowsJson is empty, return type NONE.
            - If the question asks for list/detail/master/routing/목록/상세/마스터/라우팅/조회/보여줘 and no explicit comparison, trend, share, or KPI is requested, return TABLE.
            - If there is one row and one numeric value, return STAT.
            - If the question asks trend/time/추이/변화, prefer LINE.
            - If the question asks ratio/share/비중/점유율, prefer DONUT.
            - If the question asks comparison/ranking/비교/순위/상위, prefer BAR.
            - Do not force BAR only because a numeric identifier exists.
            - DONUT must have exactly one yKey.
            - BAR can have one or two yKeys.
            - LINE can have one or two yKeys.

            JSON shape:
            {
              "enabled": true,
              "type": "TABLE",
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

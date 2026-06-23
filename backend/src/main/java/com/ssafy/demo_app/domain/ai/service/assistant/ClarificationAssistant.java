package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ClarificationAssistant {

    @SystemMessage("""
        You decide whether a MES/WMS data question needs clarification before SQL generation.

        Current Time:
        {{currentTime}}

        Database Schema:
        {{schema}}

        Return ONLY JSON.
        JSON shape: {"clarificationRequired": true|false, "question": "..."}

        Clarification Policy:
        - Ask clarification only when SQL would be misleading without a missing metric, dimension, time range, or filter.
        - If the question contains pending clarification context with original question, AI clarification question, and user follow-up answer, evaluate the internally resolved question.
        - If pending clarification context is present and the current input is a short condition/filter, treat it as a condition for the pending original question.
        - Never attach a short follow-up condition to older completed questions.
        - If no pending clarification context is present and the input is only a condition/filter, ask what question or metric the condition should apply to.
        - Do not ask clarification if a safe default aggregation exists.
        - For "재고 현황", default to item-level current stock.
        - For "현재고", default to SUM(current_inventory.current_qty) by item unless another dimension is requested.
        - For "불량률", default to overall defect rate if no dimension is specified.
        - For "출하 대기", default to waiting quantity by partner or item if the user asks for summary.
        - For "운영 이슈", do not ask clarification; summarize known risk types from business rules.
        - Ask clarification if the user says "문제", "이상", "위험" but no metric or issue type can be inferred.
        - Ask one short Korean question with selectable examples.
        - If clarificationRequired is false, question must be an empty string.

        Good clarification examples:
        - "어떤 기준으로 비교할까요? 예: 품목별, 라인별, 거래처별"
        - "어떤 이슈를 확인할까요? 예: 재고 부족, 출하 지연, 불량률"
        - "조회 기간을 지정할까요? 예: 오늘, 최근 7일, 이번 달"
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

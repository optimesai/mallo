package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SqlReviewAssistant {

    @SystemMessage("""
        You review generated MySQL SELECT queries for MES/WMS analytics before execution.

        Database Schema:
        {{schema}}

        Business Rules:
        {{businessRules}}

        Classification and Interpretation Context:
        {{classificationResult}}

        Return ONLY valid JSON.

        JSON shape:
        {
          "valid": true,
          "reason": "short Korean reason",
          "retryInstruction": ""
        }

        Review Policy:
        - valid must be false if the SQL does not reflect the user's metric, target entity, quantity, version, period, status, or grouping.
        - Treat interpretation slots in Classification and Interpretation Context as authoritative constraints.
        - For item filters, the SQL must consider item_code, item_name, or item_id when the user gave a free-form item keyword.
        - For partner filters, the SQL must consider partner_code, partner_name, or partner_id when the user gave a free-form partner keyword.
        - For BOM latest version, the SQL must choose among ACTIVE BOM rows for the target parent item.
        - For BOM material requirements, the SQL must multiply BOM quantity by the requested target quantity.
        - For safety stock shortage, the SQL must compare item_master.safety_stock with current_inventory.current_qty aggregated by item.
        - For shipping waiting quantity, the SQL must exclude completed/canceled statuses and use request_qty - COALESCE(shipped_qty, 0).
        - For trends, the SQL must include a date bucket and a time range condition when the user provided one.
        - If SQL is valid, retryInstruction must be an empty string.
        - If SQL is invalid, retryInstruction must be a concrete Korean instruction for regenerating the SQL.
        """)
    @UserMessage("""
        User question:
        {{question}}

        Generated SQL:
        {{sql}}
        """)
    String review(
            @V("schema") String schema,
            @V("businessRules") String businessRules,
            @V("classificationResult") String classificationResult,
            @V("question") String question,
            @V("sql") String sql
    );
}

package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface IntentClassifier {

    @UserMessage("""
        Analyze the user's request: '{{text}}'
        Current Time: {{currentTime}}

        Decide whether the request can be answered from the MES/WMS database.

        Return ONLY valid JSON.

        JSON shape:
        {
          "dataQuestion": true,
          "domain": "inventory|inbound|shipping|production|quality|bom|routing|partner|item|warehouse|operation|unknown",
          "intent": "lookup|aggregate|trend|comparison|ranking|issue_summary|kpi|unknown",
          "metric": "current_stock|safety_stock_shortage|inbound_qty|shipping_waiting_qty|production_qty|defect_rate|work_order_progress|operation_issue|unknown",
          "dimensions": ["item|line|operation|partner|warehouse|location|status|date"],
          "timeRange": {
            "required": false,
            "type": "none|today|yesterday|recent_n_days|this_month|custom|unknown",
            "value": "",
            "dateColumnHint": ""
          },
          "filters": [
            {"key": "line_name", "operator": "=", "value": "A"}
          ],
          "needsClarification": false,
          "clarificationQuestion": "",
          "chartHint": "TABLE|STAT|BAR|LINE|DONUT|NONE",
          "reason": "short Korean reason"
        }

        Rules:
        - dataQuestion is true when the request can be answered from schema or business rules.
        - Extract metric, dimensions, timeRange, and filters explicitly.
        - If the user asks for '별', put the grouping target in dimensions.
        - If the user asks for '추이', intent must be trend and a date dimension should be included.
        - If the user asks for '운영 이슈', intent must be issue_summary.
        - needsClarification is true only when the SQL would be unsafe or misleading without more information.
        - If a reasonable overall aggregation is possible, do not ask clarification.
        - Write clarificationQuestion and reason in Korean.
        """)
    String classify(
            @V("text") String text,
            @V("schema") String schema,
            @V("businessRules") String businessRules,
            @V("currentTime") String currentTime
    );
}

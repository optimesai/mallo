package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface IntentClassifier {

    @UserMessage("""
            Analyze the user's request: '{{text}}'
            Current Time: {{currentTime}}

            Decide whether the request is related to manufacturing, logistics, inventory,
            inbound, shipping, production, defect, line, routing, BOM, item, partner, warehouse,
            or operational issue data.

            Business Rules:
            {{businessRules}}
            
            Schema:
            {{schema}}

            Return ONLY valid JSON.
            JSON shape:
            {
              "dataQuestion": true,
              "domain": "inventory|inbound|shipping|production|quality|bom|routing|partner|item|warehouse|operation|unknown",
              "intent": "lookup|aggregate|trend|comparison|ranking|issue_summary|unknown",
              "needsClarification": false,
              "clarificationQuestion": "",
              "reason": "short Korean reason"
            }

            Rules:
            - dataQuestion is true when the request can be answered from the schema or business rules.
            - dataQuestion is false only when the request is unrelated to the database domain.
            - needsClarification is true only when a required metric, dimension, or status cannot be safely inferred.
            - If needsClarification is false, clarificationQuestion must be an empty string.
            - Write clarificationQuestion and reason in Korean.
            """)
    String classify(
            @V("text") String text,
            @V("schema") String schema,
            @V("businessRules") String businessRules,
            @V("currentTime") String currentTime
    );
}

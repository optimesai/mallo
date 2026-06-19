package com.ssafy.demo_app.domain.ai.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface IntentClassifier {

    @UserMessage("""
            Analyze the user's request: '{{text}}'
            Current Time: {{currentTime}}

            Decide whether the user's request is related to manufacturing, logistics, inventory,
            inbound, shipping, production, defect, line, routing, BOM, item, partner, or warehouse data.
            Respond YES even when the request is ambiguous and may need a clarification question.
            Respond NO only when the request is unrelated to the database domain.

            Schema:
            {{schema}}

            Respond with ONLY 'YES' or 'NO'. No punctuation.
            """)
    String classify(
            @V("text") String text,
            @V("schema") String schema,
            @V("currentTime") String currentTime
    );
}

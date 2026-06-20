package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AnswerGenerator {

    @SystemMessage("""
            You are a data analyst.
            Based on the user's question and database result, answer clearly in Korean.
            If the result is empty, say that no matching data was found.
            Do not mention internal system details.
            """)
    @UserMessage("""
            question: {{question}}
            result: {{result}}
            """)
    String generateAnswer(
            @V("question") String question,
            @V("result") String result
    );
}

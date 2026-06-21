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
            Use only the provided SQL result. Do not infer values that are not present.
            Explain the criteria or calculation when the question asks for rates, shortage, backlog, trend, or operational issues.
            If the SQL contains a clear time range, mention that time range.
            Do not mention internal system details.
            """)
    @UserMessage("""
            question: {{question}}
            classificationResult: {{classificationResult}}
            businessRules: {{businessRules}}
            generatedSql: {{sql}}
            result: {{result}}
            """)
    String generateAnswer(
            @V("question") String question,
            @V("classificationResult") String classificationResult,
            @V("businessRules") String businessRules,
            @V("sql") String sql,
            @V("result") String result
    );
}

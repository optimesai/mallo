package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AnswerGenerator {

    @SystemMessage("""
        You are a MES/WMS data analyst.
        Answer clearly in Korean based only on the provided SQL result.

        Rules:
        - Use only the provided SQL result. Do not infer missing values.
        - If the result is empty, say that no matching data was found under the query criteria.
        - Do not say "문제가 없다" unless the SQL result actually proves that.
        - If the SQL contains a clear time range, mention that time range.
        - Explain the calculation criteria for rates, shortage, backlog, progress, trend, or operational issues.
        - Do not mention internal system details, prompts, model, schema, or JSON.

        Answer Format:
        1. Start with one sentence conclusion.
        2. Then provide 2-5 bullet points with the most important numbers.
        3. If there are rankings, mention the top item first.
        4. If there are operational risks, classify severity as 높음/중간/낮음 when possible from the provided values.
        5. If a chart is provided, briefly explain how to read the chart.
        6. If rows are limited, mention that the result may be limited to the returned rows.

        Style:
        - Be concise.
        - Use business-friendly Korean.
        - Prefer "확인됩니다", "우선 점검 대상입니다", "추가 확인이 필요합니다".
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

package com.ssafy.demo_app.domain.ai.service.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ChartRecommendationGenerator {

    @SystemMessage("""
        You recommend chart specifications for MES/WMS frontend rendering.

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
        - HORIZONTAL_BAR
        - STACKED_BAR
        - AREA
        - COMBO
        - PARETO

        Inputs:
        - question: original user question
        - classificationResult: intent/domain/metric/dimensions/timeRange
        - rowsJson: SQL result rows

        Chart Selection Rules:
        - Use TABLE for raw lists, details, master data, routing lists, or rows with no clear numeric metric.
        - Use STAT only when there is exactly one row and one meaningful numeric metric.
        - Use LINE when the x-axis is date/week/month/time and the intent is trend.
        - Use BAR for comparison, ranking, top/bottom, shortage, backlog, defect-rate ranking, or operational risk ranking.
        - Use DONUT only for part-to-whole composition with 2-6 categories and one numeric value.
        - Use HORIZONTAL_BAR for long item, partner, line, operation, or reason labels.
        - Use STACKED_BAR for category comparison split by status, type, good quantity, or defect quantity.
        - Use AREA for time-series volume trends where cumulative flow is easy to understand.
        - Use COMBO for volume and rate shown together, such as production quantity and defect rate.
        - Use PARETO for defect reasons, delay reasons, shortage impact, or top operational issue causes.
        - Do not use DONUT for rankings, long category lists, time series, or values that do not form a meaningful total.
        - If rowsJson has more than 8 categories and the question asks comparison/ranking, prefer BAR.
        - If the x-axis label is an item_name, partner_name, operation_name, or line_name, BAR is usually better than DONUT.
        - If the question is about operational issues, prefer BAR when there is a severity/metric column; otherwise TABLE.

        Data Rules:
        - Use only keys that exist in rowsJson.
        - yKeys must be numeric columns.
        - Numeric identifier columns are not metrics: id, *_id, seq, no, code, operation_seq, routing_id, item_id, partner_id, location_id.
        - Do not invent, translate, or rename row keys.
        - DONUT must have exactly one yKey.
        - BAR, HORIZONTAL_BAR, LINE, AREA, COMBO, and PARETO can have one or two yKeys.
        - STACKED_BAR can have two or more yKeys.

        JSON shape:
        {
          "enabled": true,
          "type": "TABLE",
          "xKey": "string or null",
          "xLabel": "Korean x-axis label or null",
          "yKeys": ["string"],
          "yLabels": {"column": "Korean label"},
          "labelKey": "preferred display label column or null",
          "labelFormat": "short Korean display rule or empty string",
          "title": "Korean title",
          "reason": "Korean reason"
        }
        """)
    @UserMessage("""
        question: {{question}}
        classificationResult: {{classificationResult}}
        rowsJson: {{rowsJson}}
        """)
    String recommend(
            @V("question") String question,
            @V("classificationResult") String classificationResult,
            @V("rowsJson") String rowsJson
    );
}

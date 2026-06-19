package com.ssafy.demo_app.api.ai.dto;

import com.ssafy.demo_app.domain.ai.entity.AiQueryHistory;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiQueryResponse {

    private Integer queryId;
    private String question;
    private String generatedSql;
    private List<Map<String, Object>> rows;
    private Integer rowCount;
    private String answer;
    private AiQueryHistory.ExecutionStatus executionStatus;
    private AiChartResponse chart;
    private Boolean clarificationRequired;
    private String clarificationQuestion;
}

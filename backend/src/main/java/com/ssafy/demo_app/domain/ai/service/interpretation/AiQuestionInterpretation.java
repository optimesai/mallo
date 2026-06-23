package com.ssafy.demo_app.domain.ai.service.interpretation;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiQuestionInterpretation {

    private boolean dataQuestion;
    private String domain = "unknown";
    private String intent = "unknown";
    private String metric = "unknown";
    private List<String> dimensions = List.of();
    private String timeRange = "";
    private String itemKeyword = "";
    private String partnerKeyword = "";
    private String warehouseKeyword = "";
    private String locationKeyword = "";
    private String lineKeyword = "";
    private String operationKeyword = "";
    private String statusKeyword = "";
    private Integer targetQty;
    private String bomVersionPolicy = "UNSPECIFIED";
    private String bomVersion = "";
    private String summary = "";
    private List<String> suggestedQuestions = List.of();

    public static AiQuestionInterpretation unknown() {
        AiQuestionInterpretation interpretation = new AiQuestionInterpretation();
        interpretation.setSummary("데이터 질의로 해석할 수 있는 도메인 단서가 부족합니다.");
        interpretation.setSuggestedQuestions(List.of(
                "안전재고 미만 품목을 보여줘",
                "최근 7일 입고 수량 추이를 알려줘",
                "출하 대기 건수를 거래처별로 집계해줘"
        ));
        return interpretation;
    }
}

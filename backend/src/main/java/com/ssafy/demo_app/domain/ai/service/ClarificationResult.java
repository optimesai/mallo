package com.ssafy.demo_app.domain.ai.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClarificationResult {

    private boolean clarificationRequired;
    private String question;

    public static ClarificationResult required(String question) {
        ClarificationResult result = new ClarificationResult();
        result.setClarificationRequired(true);
        result.setQuestion(question);
        return result;
    }

    public static ClarificationResult notRequired() {
        ClarificationResult result = new ClarificationResult();
        result.setClarificationRequired(false);
        result.setQuestion("");
        return result;
    }
}

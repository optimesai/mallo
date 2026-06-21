package com.ssafy.demo_app.domain.ai.service.classification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiIntentResult {

    private boolean dataQuestion;
    private String domain = "";
    private String intent = "";
    private boolean needsClarification;
    private String clarificationQuestion = "";
    private String reason = "";

    public static AiIntentResult dataQuestion() {
        AiIntentResult result = new AiIntentResult();
        result.setDataQuestion(true);
        return result;
    }

    public static AiIntentResult notDataQuestion() {
        return new AiIntentResult();
    }
}

package com.ssafy.demo_app.domain.ai.service.classification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.demo_app.domain.ai.service.assistant.IntentClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IntentClassificationService {

    private final IntentClassifier intentClassifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiIntentResult classify(String question, String schema, String businessRules, String currentTime) {
        String rawResult = intentClassifier.classify(question, schema, businessRules, currentTime);
        return parseResult(rawResult);
    }

    private AiIntentResult parseResult(String rawResult) {
        if (rawResult == null || rawResult.isBlank()) {
            return AiIntentResult.notDataQuestion();
        }

        try {
            AiIntentResult result = objectMapper.readValue(cleanJson(rawResult), AiIntentResult.class);
            normalize(result);
            return result;
        } catch (JsonProcessingException exception) {
            return parseLegacyResult(rawResult);
        }
    }

    private void normalize(AiIntentResult result) {
        if (result.getDomain() == null) {
            result.setDomain("");
        }
        if (result.getIntent() == null) {
            result.setIntent("");
        }
        if (result.getClarificationQuestion() == null) {
            result.setClarificationQuestion("");
        }
        if (result.getReason() == null) {
            result.setReason("");
        }
    }

    private AiIntentResult parseLegacyResult(String rawResult) {
        String normalized = rawResult.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (normalized.contains("YES")) {
            return AiIntentResult.dataQuestion();
        }
        return AiIntentResult.notDataQuestion();
    }

    private String cleanJson(String rawResult) {
        return rawResult
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}

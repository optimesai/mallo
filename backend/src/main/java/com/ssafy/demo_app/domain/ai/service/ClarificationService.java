package com.ssafy.demo_app.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClarificationService {

    private final ClarificationCandidateService clarificationCandidateService;
    private final ClarificationAssistant clarificationAssistant;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClarificationResult evaluate(String question, String schema, String currentTime) {
        if (!clarificationCandidateService.isCandidate(question)) {
            return ClarificationResult.notRequired();
        }

        try {
            String rawResult = clarificationAssistant.evaluate(schema, question, currentTime);
            return parseResult(rawResult);
        } catch (Exception exception) {
            return ClarificationResult.notRequired();
        }
    }

    private ClarificationResult parseResult(String rawResult) throws JsonProcessingException {
        if (rawResult == null || rawResult.isBlank()) {
            return ClarificationResult.notRequired();
        }

        ClarificationResult result = objectMapper.readValue(cleanJson(rawResult), ClarificationResult.class);
        if (!result.isClarificationRequired() || result.getQuestion() == null || result.getQuestion().isBlank()) {
            return ClarificationResult.notRequired();
        }
        return ClarificationResult.required(result.getQuestion());
    }

    private String cleanJson(String rawResult) {
        return rawResult
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}

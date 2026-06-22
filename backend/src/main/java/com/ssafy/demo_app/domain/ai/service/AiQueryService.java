package com.ssafy.demo_app.domain.ai.service;

import com.ssafy.demo_app.api.ai.dto.AiQueryRequest;
import com.ssafy.demo_app.api.ai.dto.AiQueryResponse;

public interface AiQueryService {

    AiQueryResponse ask(Integer userId, AiQueryRequest request);
}

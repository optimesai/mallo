package com.ssafy.demo_app.api.ai;

import com.ssafy.demo_app.api.ai.dto.AiQueryRequest;
import com.ssafy.demo_app.api.ai.dto.AiQueryResponse;
import com.ssafy.demo_app.domain.ai.service.AiQueryService;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/queries")
@RequiredArgsConstructor
public class AiQueryController {

    private final AiQueryService aiQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<AiQueryResponse>> ask(
            @Valid @RequestBody AiQueryRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AiQueryResponse response = aiQueryService.ask(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

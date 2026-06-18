package com.ssafy.demo_app.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiQueryRequest {

    @NotBlank
    private String question;
}

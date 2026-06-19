package com.ssafy.demo_app.domain.ai.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FewShotExample {

    private String id;
    private String category;
    private String question;
    private String sql;
    private String notes;
}

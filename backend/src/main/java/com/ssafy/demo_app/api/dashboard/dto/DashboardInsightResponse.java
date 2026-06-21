package com.ssafy.demo_app.api.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardInsightResponse {

    private String id;
    private String title;
    private String description;
    private String severity;
    private String source;
}

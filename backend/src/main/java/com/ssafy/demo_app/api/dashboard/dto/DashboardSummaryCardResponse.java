package com.ssafy.demo_app.api.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardSummaryCardResponse {

    private String id;
    private String label;
    private String value;
    private String caption;
    private String severity;
}

package com.ssafy.demo_app.domain.dashboard.service;

import com.ssafy.demo_app.api.dashboard.dto.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getSummary(String period);
}

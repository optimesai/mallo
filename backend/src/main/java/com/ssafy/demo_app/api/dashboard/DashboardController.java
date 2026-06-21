package com.ssafy.demo_app.api.dashboard;

import com.ssafy.demo_app.api.dashboard.dto.DashboardSummaryResponse;
import com.ssafy.demo_app.domain.dashboard.service.DashboardService;
import com.ssafy.demo_app.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestParam(defaultValue = "7d") String period
    ) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary(period)));
    }
}

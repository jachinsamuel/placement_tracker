package com.placement.controller;

import com.placement.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Placement Officer Dashboard and Analytics metrics.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOverallStats() {
        return ResponseEntity.ok(dashboardService.getOverallStats());
    }

    @GetMapping("/department-stats")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentDistribution() {
        return ResponseEntity.ok(dashboardService.getDepartmentDistribution());
    }

    @GetMapping("/company-stats")
    public ResponseEntity<List<Map<String, Object>>> getCompanyEligibilityStats() {
        return ResponseEntity.ok(dashboardService.getCompanyEligibilityStats());
    }
}

package com.library.management.controller;

import com.library.management.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getDashboardStats());
        model.addAttribute("recentIssues", dashboardService.getRecentIssues());
        model.addAttribute("overdueIssues", dashboardService.getOverdueIssues());
        model.addAttribute("activeNav", "dashboard");
        return "dashboard";
    }
}

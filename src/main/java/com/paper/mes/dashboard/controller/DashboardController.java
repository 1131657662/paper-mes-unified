package com.paper.mes.dashboard.controller;

import com.paper.mes.common.R;
import com.paper.mes.dashboard.dto.DashboardOverviewVO;
import com.paper.mes.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public R<DashboardOverviewVO> overview() {
        return R.success(dashboardService.overview());
    }
}

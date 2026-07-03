package com.paper.mes.report.controller;

import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequirePermission(Permissions.REPORT_VIEW)
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    public R<ReportOverviewVO> overview(ReportQuery query) {
        return R.success(reportService.overview(query));
    }

    @GetMapping("/dimensions")
    public R<List<ReportDimensionVO>> dimensions(ReportQuery query) {
        return R.success(reportService.dimensionSummary(query));
    }

    @GetMapping("/details")
    public R<List<ReportDetailVO>> details(ReportQuery query) {
        return R.success(reportService.detailRows(query));
    }

    @GetMapping("/export")
    public void export(ReportQuery query, HttpServletResponse response) {
        reportService.exportWorkbook(query, response);
    }
}

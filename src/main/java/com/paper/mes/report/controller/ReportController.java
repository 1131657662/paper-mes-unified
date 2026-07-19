package com.paper.mes.report.controller;

import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.report.dto.ReportDetailsVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import jakarta.validation.Valid;
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
    public R<ReportOverviewVO> overview(@Valid ReportQuery query) {
        return R.success(reportService.overview(query));
    }

    @GetMapping("/dimensions")
    public R<List<ReportDimensionVO>> dimensions(@Valid ReportQuery query) {
        return R.success(reportService.dimensionSummary(query));
    }

    @GetMapping("/details")
    public R<ReportDetailsVO> details(@Valid ReportQuery query) {
        return R.success(reportService.detailRows(query));
    }

}

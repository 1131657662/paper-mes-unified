package com.paper.mes.report.controller;

import com.paper.mes.common.R;
import com.paper.mes.report.dto.CustomerReportVO;
import com.paper.mes.report.dto.LossReportVO;
import com.paper.mes.report.dto.MachineReportVO;
import com.paper.mes.report.dto.MonthlyReportVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public R<List<MonthlyReportVO>> monthly(ReportQuery query) {
        return R.success(reportService.monthlySummary(query));
    }

    @GetMapping("/customer")
    public R<List<CustomerReportVO>> customer(ReportQuery query) {
        return R.success(reportService.customerSummary(query));
    }

    @GetMapping("/loss")
    public R<List<LossReportVO>> loss(ReportQuery query) {
        return R.success(reportService.lossAnalysis(query));
    }

    @GetMapping("/machine")
    public R<List<MachineReportVO>> machine(ReportQuery query) {
        return R.success(reportService.machineOutput(query));
    }
}

package com.paper.mes.report.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.report.dto.ReportCollectionAnalysisVO;
import com.paper.mes.report.dto.ReportDeliveryAnalysisVO;
import com.paper.mes.report.dto.ReportInventoryAnalysisVO;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportOperationalAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/topics")
@RequirePermission(Permissions.REPORT_VIEW)
@RequiredArgsConstructor
public class ReportOperationalController {
    private final ReportOperationalAnalysisService service;

    @PostMapping("/settlement/query")
    public R<ReportSettlementAnalysisVO> settlement(@Valid @RequestBody ReportQuery query) {
        return R.success(service.settlement(query));
    }

    @PostMapping("/collection/query")
    public R<ReportCollectionAnalysisVO> collection(@Valid @RequestBody ReportQuery query) {
        return R.success(service.collection(query));
    }

    @PostMapping("/inventory/query")
    public R<ReportInventoryAnalysisVO> inventory(@Valid @RequestBody ReportQuery query) {
        return R.success(service.inventory(query));
    }

    @PostMapping("/delivery/query")
    public R<ReportDeliveryAnalysisVO> delivery(@Valid @RequestBody ReportQuery query) {
        return R.success(service.delivery(query));
    }
}

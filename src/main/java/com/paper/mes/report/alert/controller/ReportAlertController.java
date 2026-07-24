package com.paper.mes.report.alert.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.report.alert.dto.ReportThresholdContextVO;
import com.paper.mes.report.alert.service.ReportAlertThresholdService;
import com.paper.mes.report.dto.ReportQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report-alerts")
@RequirePermission(Permissions.REPORT_VIEW)
@RequiredArgsConstructor
public class ReportAlertController {
    private final ReportAlertThresholdService thresholdService;

    @GetMapping("/threshold-context")
    public R<ReportThresholdContextVO> thresholdContext(@Valid ReportQuery query) {
        return R.success(thresholdService.resolve(query));
    }
}

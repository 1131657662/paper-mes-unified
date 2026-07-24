package com.paper.mes.report.alert.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.report.alert.dto.ReportAlertRuleSaveDTO;
import com.paper.mes.report.alert.dto.ReportAlertRuleVO;
import com.paper.mes.report.alert.service.ReportAlertRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/report-alert-rules")
@RequirePermission(Permissions.SYSTEM_CONFIG)
@RequiredArgsConstructor
public class ReportAlertRuleController {
    private static final String UUID_PATTERN = "^[0-9a-fA-F-]{32,36}$|^rpt-alert-[a-z0-9-]+$";
    private final ReportAlertRuleService ruleService;

    @GetMapping
    public R<List<ReportAlertRuleVO>> list() {
        return R.success(ruleService.list());
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody ReportAlertRuleSaveDTO dto) {
        return R.success(ruleService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @Valid @RequestBody ReportAlertRuleSaveDTO dto) {
        ruleService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @RequestParam @Min(1) int version) {
        ruleService.delete(uuid, version);
        return R.success();
    }
}

package com.paper.mes.report.alert.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.report.alert.dto.ReportAlertEventPageVO;
import com.paper.mes.report.alert.dto.ReportAlertEventQuery;
import com.paper.mes.report.alert.service.ReportAlertEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/report-alert-events")
@RequiredArgsConstructor
public class ReportAlertEventController {
    private static final String UUID_PATTERN = "^[0-9a-fA-F-]{32,36}$";
    private final ReportAlertEventService eventService;

    @GetMapping
    @RequirePermission(Permissions.REPORT_VIEW)
    public R<ReportAlertEventPageVO> page(@Valid ReportAlertEventQuery query) {
        return R.success(eventService.page(query));
    }

    @PostMapping("/{uuid}/acknowledge")
    @RequirePermission(Permissions.SYSTEM_CONFIG)
    public R<Void> acknowledge(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid) {
        eventService.acknowledge(uuid);
        return R.success();
    }

    @PostMapping("/{uuid}/ignore")
    @RequirePermission(Permissions.SYSTEM_CONFIG)
    public R<Void> ignore(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @Valid @RequestBody IgnoreRequest request) {
        eventService.ignore(uuid, request.reason());
        return R.success();
    }

    public record IgnoreRequest(@Size(min = 1, max = 500) String reason) {
    }
}

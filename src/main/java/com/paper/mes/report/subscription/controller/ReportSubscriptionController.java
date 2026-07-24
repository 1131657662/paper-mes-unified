package com.paper.mes.report.subscription.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRecipientVO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionSaveDTO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionVO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRunPageVO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRunQuery;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunCommandService;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunQueryService;
import com.paper.mes.report.subscription.service.ReportSubscriptionService;
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
@RequestMapping("/api/report-subscriptions")
@RequirePermission(Permissions.REPORT_VIEW)
@RequiredArgsConstructor
public class ReportSubscriptionController {
    private static final String UUID_PATTERN = "^[0-9a-fA-F-]{32,36}$";
    private final ReportSubscriptionService subscriptionService;
    private final ReportSubscriptionRunQueryService runQueryService;
    private final ReportSubscriptionRunCommandService runCommandService;

    @GetMapping
    public R<List<ReportSubscriptionVO>> list() {
        return R.success(subscriptionService.listMine());
    }

    @GetMapping("/recipient-candidates")
    public R<List<ReportSubscriptionRecipientVO>> recipientCandidates() {
        return R.success(subscriptionService.recipientCandidates());
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody ReportSubscriptionSaveDTO dto) {
        return R.success(subscriptionService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @Valid @RequestBody ReportSubscriptionSaveDTO dto) {
        subscriptionService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @RequestParam @Min(1) int version) {
        subscriptionService.delete(uuid, version);
        return R.success();
    }

    @GetMapping("/{uuid}/runs")
    public R<ReportSubscriptionRunPageVO> runs(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
            @Valid ReportSubscriptionRunQuery query) {
        return R.success(runQueryService.page(uuid, query));
    }

    @PostMapping("/{uuid}/run-now")
    public R<String> runNow(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid) {
        return R.success(runCommandService.runNow(uuid));
    }

    @PostMapping("/{uuid}/runs/{runUuid}/retry")
    public R<String> retryRun(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                              @PathVariable @Pattern(regexp = UUID_PATTERN) String runUuid) {
        return R.success(runCommandService.retry(uuid, runUuid));
    }
}

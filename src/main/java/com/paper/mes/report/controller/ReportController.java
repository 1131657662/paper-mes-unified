package com.paper.mes.report.controller;

import com.paper.mes.common.R;
import com.paper.mes.common.PageResult;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.report.dto.ReportDetailQuery;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportPageAnalysisVO;
import com.paper.mes.report.dto.ReportDimensionAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportProductionAnalysisVO;
import com.paper.mes.report.dto.ReportQualityLossAnalysisVO;
import com.paper.mes.report.dto.ReportMetricContextVO;
import com.paper.mes.report.dto.ReportMetricReleaseDetailVO;
import com.paper.mes.report.dto.ReportMetricReleaseSummaryVO;
import com.paper.mes.report.service.ReportMetricCatalogService;
import com.paper.mes.report.service.ReportService;
import com.paper.mes.report.service.ReportQueryCoordinator;
import com.paper.mes.report.service.ReportQuerySnapshotService;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/reports")
@RequirePermission(Permissions.REPORT_VIEW)
@RequiredArgsConstructor
public class ReportController {

    private static final String UUID_PATTERN = "^(?:[0-9a-fA-F]{32}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
            + "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$";
    private static final java.util.regex.Pattern UUID_FORMAT = java.util.regex.Pattern.compile(UUID_PATTERN);

    private final ReportService reportService;
    private final ReportMetricCatalogService metricCatalogService;
    private final ReportQueryCoordinator queryCoordinator;
    private final ReportQuerySnapshotService querySnapshotService;

    @GetMapping("/metric-context")
    public R<ReportMetricContextVO> metricContext() {
        return R.success(metricCatalogService.activeContext());
    }

    @GetMapping("/metric-releases")
    public R<List<ReportMetricReleaseSummaryVO>> metricReleases() {
        return R.success(metricCatalogService.releaseHistory());
    }

    @GetMapping("/metric-releases/{uuid}")
    public R<ReportMetricReleaseDetailVO> metricRelease(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String uuid) {
        return R.success(metricCatalogService.releaseDetail(requireReleaseUuid(uuid)));
    }

    @GetMapping("/overview")
    public R<ReportOverviewVO> overview(@Valid ReportQuery query) {
        return R.success(reportService.overview(query));
    }

    @PostMapping("/query")
    public R<ReportPageAnalysisVO> pageAnalysis(@Valid @RequestBody ReportDetailQuery query) {
        return R.success(reportService.pageAnalysis(query));
    }

    @PostMapping("/dimension-query")
    public R<ReportDimensionAnalysisVO> dimensionAnalysis(@Valid @RequestBody ReportQuery query) {
        return R.success(reportService.dimensionAnalysis(query));
    }

    @PostMapping("/topics/production/query")
    public R<ReportProductionAnalysisVO> productionAnalysis(
            @Valid @RequestBody ReportQuery query) {
        return R.success(reportService.productionAnalysis(query));
    }

    @PostMapping("/topics/quality-loss/query")
    public R<ReportQualityLossAnalysisVO> qualityLossAnalysis(
            @Valid @RequestBody ReportQuery query) {
        return R.success(reportService.qualityLossAnalysis(query));
    }

    @GetMapping("/query-metadata")
    public R<ReportQueryExecutionMetaVO> queryMetadata(@Valid ReportQuery query) {
        return R.success(queryCoordinator.prepare(query));
    }

    @PostMapping("/query-snapshots")
    public R<ReportQuerySnapshotVO> createQuerySnapshot(@Valid @RequestBody ReportQuery query) {
        return R.success(querySnapshotService.create(query));
    }

    @GetMapping("/query-snapshots/{uuid}")
    public R<ReportQuerySnapshotVO> querySnapshot(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String uuid) {
        return R.success(querySnapshotService.requireAccessible(uuid));
    }

    @GetMapping("/dimensions")
    public R<List<ReportDimensionVO>> dimensions(@Valid ReportQuery query) {
        return R.success(reportService.dimensionSummary(query));
    }

    @GetMapping("/details")
    public R<PageResult<ReportDetailVO>> details(@Valid ReportDetailQuery query) {
        return R.success(reportService.detailRows(query));
    }

    @GetMapping("/candidates/papers")
    public R<List<String>> paperCandidates(
            @RequestParam @Size(max = 100) String keyword) {
        return R.success(reportService.paperCandidates(keyword));
    }

    private String requireReleaseUuid(String uuid) {
        if (!UUID_FORMAT.matcher(uuid).matches()) {
            throw new IllegalArgumentException("指标发布包 UUID 格式错误");
        }
        return uuid;
    }

}

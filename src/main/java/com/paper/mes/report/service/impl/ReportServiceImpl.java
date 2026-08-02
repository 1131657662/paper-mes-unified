package com.paper.mes.report.service.impl;

import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.report.dto.ReportDetailQuery;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportDimensionAnalysisVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportPageAnalysisVO;
import com.paper.mes.report.dto.ReportProductionAnalysisVO;
import com.paper.mes.report.dto.ReportQualityLossAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportExportExecutionRequest;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportExportConsistencyGuard;
import com.paper.mes.report.service.ReportAmountVisibility;
import com.paper.mes.report.service.ReportDimensionPolicy;
import com.paper.mes.report.service.ReportQueryCoordinator;
import com.paper.mes.report.service.ReportService;
import com.paper.mes.report.service.ReportTopicDimensionSorter;
import com.paper.mes.report.service.ReportWorkbookExportCoordinator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportMapper reportMapper;
    private final ReportQueryCoordinator queryCoordinator;
    private final ReportExportConsistencyGuard exportConsistencyGuard;
    private final ReportWorkbookExportCoordinator workbookExportCoordinator;
    private final ReportAmountVisibility amountVisibility;

    @Override
    public ReportOverviewVO overview(ReportQuery query) {
        prepare(query);
        ReportOverviewVO result = reportMapper.overview(query);
        amountVisibility.redactOverview(result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportPageAnalysisVO pageAnalysis(ReportDetailQuery query) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(query);
        ReportOverviewVO overview = reportMapper.overview(query);
        List<ReportDimensionVO> current = reportMapper.dimensionSummary(query, dimensionOf(query));
        PageResult<ReportDetailVO> details = detailPage(query);
        List<ReportDimensionVO> monthly = reportMapper.dimensionSummary(query, "month");
        List<ReportDimensionVO> customers = reportMapper.dimensionSummary(query, "customer");
        List<ReportDimensionVO> papers = reportMapper.dimensionSummary(query, "paper");
        amountVisibility.redactOverview(overview);
        amountVisibility.redactDimensions(current);
        amountVisibility.redactDimensions(monthly);
        amountVisibility.redactDimensions(customers);
        amountVisibility.redactDimensions(papers);
        amountVisibility.redactDetails(details.getRecords());
        return new ReportPageAnalysisVO(execution, overview, current, details, monthly, customers, papers);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDimensionAnalysisVO dimensionAnalysis(ReportQuery query) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(query);
        List<ReportDimensionVO> rows = reportMapper.dimensionSummary(query, dimensionOf(query));
        amountVisibility.redactDimensions(rows);
        return new ReportDimensionAnalysisVO(execution, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportProductionAnalysisVO productionAnalysis(ReportQuery query) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(query);
        ReportOverviewVO overview = reportMapper.overview(query);
        List<ReportDimensionVO> monthly = reportMapper.dimensionSummary(query, "month");
        List<ReportDimensionVO> process = reportMapper.dimensionSummary(query, "process");
        List<ReportDimensionVO> machines = ReportTopicDimensionSorter.byInputWeight(
                reportMapper.dimensionSummary(query, "machine"));
        redactDimensions(overview, monthly, process, machines);
        return new ReportProductionAnalysisVO(
                "production",
                overview, monthly, process, machines,
                execution.dataAsOf(), execution);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportQualityLossAnalysisVO qualityLossAnalysis(ReportQuery query) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(query);
        ReportOverviewVO overview = reportMapper.overview(query);
        List<ReportDimensionVO> monthly = reportMapper.dimensionSummary(query, "month");
        List<ReportDimensionVO> papers = ReportTopicDimensionSorter.byLossRisk(
                reportMapper.dimensionSummary(query, "paper"));
        List<ReportDetailVO> leaders = reportMapper.lossLeaderRows(query, 10);
        amountVisibility.redactOverview(overview);
        amountVisibility.redactDimensions(monthly);
        amountVisibility.redactDimensions(papers);
        amountVisibility.redactDetails(leaders);
        return new ReportQualityLossAnalysisVO(
                "quality-loss",
                overview, monthly, papers, leaders,
                execution.dataAsOf(), execution);
    }

    @Override
    public List<ReportDimensionVO> dimensionSummary(ReportQuery query) {
        prepare(query);
        List<ReportDimensionVO> rows = reportMapper.dimensionSummary(query, dimensionOf(query));
        amountVisibility.redactDimensions(rows);
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReportDetailVO> detailRows(ReportDetailQuery query) {
        prepare(query);
        return detailPage(query);
    }

    private PageResult<ReportDetailVO> detailPage(ReportDetailQuery query) {
        var page = PageRequestBounds.<ReportDetailVO>of(query.getCurrent(), query.getSize());
        long total = reportMapper.detailCount(query);
        List<ReportDetailVO> rows = reportMapper.detailRows(query, page.offset(), page.getSize());
        amountVisibility.redactDetails(rows);
        PageResult<ReportDetailVO> result = new PageResult<>();
        result.setRecords(rows);
        result.setTotal(total);
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Override
    public List<String> paperCandidates(String keyword) {
        if (!StringUtils.hasText(keyword)) return List.of();
        return reportMapper.paperCandidates(keyword.trim(), 50);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportWorkbook(ReportQuery query, Path target) {
        ReportQueryExecutionMetaVO metadata = queryCoordinator.prepare(query);
        workbookExportCoordinator.writeStandard(query, target, metadata);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportWorkbook(ReportExportExecutionRequest request, Path target) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(request.query());
        exportConsistencyGuard.requireSameMetricContext(request.submissionSnapshot(), execution);
        ReportExportAuditMetadata audit = new ReportExportAuditMetadata(
                request.reportPath(), request.submissionSnapshot().querySnapshotUuid(),
                request.submissionSnapshot().dataAsOf(), execution.dataAsOf(),
                execution.metricReleaseUuid(), execution.metricVersionMap());
        workbookExportCoordinator.writeAudited(request, target, audit);
    }

    private String dimensionOf(ReportQuery query) {
        return ReportDimensionPolicy.dimensionOf(query);
    }

    private void prepare(ReportQuery query) {
        queryCoordinator.requireExecutable(query);
    }

    private void redactDimensions(ReportOverviewVO overview, List<ReportDimensionVO>... dimensions) {
        amountVisibility.redactOverview(overview);
        for (List<ReportDimensionVO> rows : dimensions) amountVisibility.redactDimensions(rows);
    }

}

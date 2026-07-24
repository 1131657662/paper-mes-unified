package com.paper.mes.report.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.report.dto.ReportDetailQuery;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportProductionAnalysisVO;
import com.paper.mes.report.dto.ReportQualityLossAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportExportExecutionRequest;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportExportService;
import com.paper.mes.report.service.ReportExportConsistencyGuard;
import com.paper.mes.report.service.ReportQueryCoordinator;
import com.paper.mes.report.service.ReportService;
import com.paper.mes.report.service.ReportTopicDimensionSorter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    static final long EXPORT_ROW_LIMIT = 100_000;

    private static final Set<String> DIMENSIONS = Set.of(
            "month", "customer", "paper", "process", "machine", "invoice", "settleType", "status");

    private final ReportMapper reportMapper;
    private final ReportExportService reportExportService;
    private final ReportQueryCoordinator queryCoordinator;
    private final ReportExportConsistencyGuard exportConsistencyGuard;

    @Override
    public ReportOverviewVO overview(ReportQuery query) {
        prepare(query);
        return reportMapper.overview(query);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportProductionAnalysisVO productionAnalysis(ReportQuery query) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(query);
        return new ReportProductionAnalysisVO(
                "production",
                reportMapper.overview(query),
                reportMapper.dimensionSummary(query, "month"),
                reportMapper.dimensionSummary(query, "process"),
                ReportTopicDimensionSorter.byInputWeight(
                        reportMapper.dimensionSummary(query, "machine")),
                execution.dataAsOf());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportQualityLossAnalysisVO qualityLossAnalysis(ReportQuery query) {
        ReportQueryExecutionMetaVO execution = queryCoordinator.prepare(query);
        return new ReportQualityLossAnalysisVO(
                "quality-loss",
                reportMapper.overview(query),
                reportMapper.dimensionSummary(query, "month"),
                ReportTopicDimensionSorter.byLossRisk(
                        reportMapper.dimensionSummary(query, "paper")),
                reportMapper.lossLeaderRows(query, 10),
                execution.dataAsOf());
    }

    @Override
    public List<ReportDimensionVO> dimensionSummary(ReportQuery query) {
        prepare(query);
        return reportMapper.dimensionSummary(query, dimensionOf(query));
    }

    @Override
    public PageResult<ReportDetailVO> detailRows(ReportDetailQuery query) {
        prepare(query);
        var page = PageRequestBounds.<ReportDetailVO>of(query.getCurrent(), query.getSize());
        long total = reportMapper.detailCount(query);
        List<ReportDetailVO> rows = reportMapper.detailRows(query, page.offset(), page.getSize());
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
        writeExport(query, target, metadata, null);
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
        writeExport(request.query(), target, execution, audit);
    }

    private void writeExport(ReportQuery query, Path target, ReportQueryExecutionMetaVO metadata,
                             ReportExportAuditMetadata audit) {
        try (OutputStream output = Files.newOutputStream(target)) {
            writeWorkbook(query, output, metadata, audit);
        } catch (IOException exception) {
            throw new BusinessException("导出统计报表失败");
        }
    }

    private void writeWorkbook(ReportQuery query, OutputStream output,
                               ReportQueryExecutionMetaVO metadata,
                               ReportExportAuditMetadata audit) throws IOException {
        String dimension = dimensionOf(query);
        requireExportCapacity(query);
        ReportOverviewVO overview = reportMapper.overview(query);
        List<ReportDimensionVO> dimensions = reportMapper.dimensionSummary(query, dimension);
        try (var details = reportMapper.detailCursor(query);
             SXSSFWorkbook workbook = workbook(overview, dimensions, details, dimension, metadata, audit)) {
            workbook.write(output);
        }
    }

    private SXSSFWorkbook workbook(ReportOverviewVO overview, List<ReportDimensionVO> dimensions,
                                    Iterable<ReportDetailVO> details, String dimension,
                                    ReportQueryExecutionMetaVO metadata, ReportExportAuditMetadata audit) {
        if (audit != null) {
            return reportExportService.buildAuditedWorkbook(overview, dimensions, details, dimension, audit);
        }
        return reportExportService.buildWorkbook(overview, dimensions, details, dimension, metadata);
    }

    private void requireExportCapacity(ReportQuery query) {
        if (reportMapper.detailCount(query) > EXPORT_ROW_LIMIT) {
            throw new BusinessException("导出明细超过10万条，请缩小日期范围或增加筛选条件");
        }
    }

    private String dimensionOf(ReportQuery query) {
        String dimension = query == null ? null : query.getDimension();
        if (!StringUtils.hasText(dimension)) return "customer";
        if (!DIMENSIONS.contains(dimension)) {
            throw new BusinessException("不支持的统计维度：" + dimension);
        }
        return dimension;
    }

    private void prepare(ReportQuery query) {
        queryCoordinator.requireExecutable(query);
    }

}

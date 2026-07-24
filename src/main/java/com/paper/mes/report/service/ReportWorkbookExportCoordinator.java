package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportExportExecutionRequest;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReportWorkbookExportCoordinator {
    static final long EXPORT_ROW_LIMIT = 100_000;
    private static final Set<String> STANDARD_PATHS = Set.of("/reports/overview", "/reports/explorer");
    private static final Set<String> TOPIC_PATHS = Set.of("/reports/production", "/reports/quality-loss");
    private static final Set<String> OPERATIONAL_PATHS = Set.of(
            "/reports/settlement", "/reports/collection", "/reports/inventory", "/reports/delivery");

    private final ReportMapper mapper;
    private final ReportExportService standardExporter;
    private final ReportTopicWorkbookExporter topicExporter;
    private final ReportOperationalWorkbookExporter operationalExporter;

    public void writeStandard(ReportQuery query, Path target, ReportQueryExecutionMetaVO metadata) {
        write(target, () -> standardWorkbook(query, metadata, null));
    }

    public void writeAudited(ReportExportExecutionRequest request, Path target,
                             ReportExportAuditMetadata metadata) {
        write(target, () -> auditedWorkbook(request.reportPath(), request.query(), metadata));
    }

    private SXSSFWorkbook auditedWorkbook(String path, ReportQuery query,
                                           ReportExportAuditMetadata metadata) {
        if (STANDARD_PATHS.contains(path)) return standardWorkbook(query, null, metadata);
        if (TOPIC_PATHS.contains(path)) return topicExporter.build(path, query, metadata);
        if (OPERATIONAL_PATHS.contains(path)) return operationalExporter.build(path, query, metadata);
        throw new BusinessException("不支持的报表导出路径");
    }

    private SXSSFWorkbook standardWorkbook(ReportQuery query, ReportQueryExecutionMetaVO metadata,
                                            ReportExportAuditMetadata audit) {
        requireExportCapacity(query);
        var overview = mapper.overview(query);
        String dimension = ReportDimensionPolicy.dimensionOf(query);
        var dimensions = mapper.dimensionSummary(query, dimension);
        try (var details = mapper.detailCursor(query)) {
            if (audit != null) {
                return standardExporter.buildAuditedWorkbook(overview, dimensions, details, dimension, audit);
            }
            return standardExporter.buildWorkbook(overview, dimensions, details, dimension, metadata);
        } catch (IOException exception) {
            throw new BusinessException("导出统计报表失败");
        }
    }

    private void requireExportCapacity(ReportQuery query) {
        if (mapper.detailCount(query) > EXPORT_ROW_LIMIT) {
            throw new BusinessException("导出明细超过10万条，请缩小日期范围或增加筛选条件");
        }
    }

    private void write(Path target, WorkbookSupplier supplier) {
        try (SXSSFWorkbook workbook = supplier.get();
             OutputStream output = Files.newOutputStream(target)) {
            workbook.write(output);
        } catch (IOException exception) {
            throw new BusinessException("导出统计报表失败");
        }
    }

    @FunctionalInterface
    private interface WorkbookSupplier {
        SXSSFWorkbook get();
    }
}

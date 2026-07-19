package com.paper.mes.report.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDetailsVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportExportService;
import com.paper.mes.report.service.ReportService;
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
    static final int DETAIL_DISPLAY_LIMIT = 1_000;
    static final long EXPORT_ROW_LIMIT = 100_000;

    private static final Set<String> DIMENSIONS = Set.of(
            "month", "customer", "paper", "process", "machine", "invoice", "settleType", "status");

    private final ReportMapper reportMapper;
    private final ReportExportService reportExportService;

    @Override
    public ReportOverviewVO overview(ReportQuery query) {
        return reportMapper.overview(query);
    }

    @Override
    public List<ReportDimensionVO> dimensionSummary(ReportQuery query) {
        return reportMapper.dimensionSummary(query, dimensionOf(query));
    }

    @Override
    public ReportDetailsVO detailRows(ReportQuery query) {
        long total = reportMapper.detailCount(query);
        List<ReportDetailVO> rows = reportMapper.detailRows(query, DETAIL_DISPLAY_LIMIT);
        return ReportDetailsVO.builder().rows(rows).total(total)
                .displayLimit(DETAIL_DISPLAY_LIMIT).truncated(total > rows.size()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportWorkbook(ReportQuery query, Path target) {
        try (OutputStream output = Files.newOutputStream(target)) {
            writeWorkbook(query, output);
        } catch (IOException exception) {
            throw new BusinessException("导出统计报表失败");
        }
    }

    private void writeWorkbook(ReportQuery query, OutputStream output) throws IOException {
        String dimension = dimensionOf(query);
        requireExportCapacity(query);
        ReportOverviewVO overview = reportMapper.overview(query);
        List<ReportDimensionVO> dimensions = reportMapper.dimensionSummary(query, dimension);
        try (var details = reportMapper.detailCursor(query);
             SXSSFWorkbook workbook = reportExportService.buildWorkbook(overview, dimensions, details, dimension)) {
            workbook.write(output);
        }
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

}

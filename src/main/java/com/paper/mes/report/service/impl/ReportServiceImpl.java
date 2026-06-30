package com.paper.mes.report.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.CustomerReportVO;
import com.paper.mes.report.dto.LossReportVO;
import com.paper.mes.report.dto.MachineReportVO;
import com.paper.mes.report.dto.MonthlyReportVO;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportExportService;
import com.paper.mes.report.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Set<String> DIMENSIONS = Set.of(
            "month", "customer", "paper", "process", "machine", "invoice", "settleType", "status"
    );

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
    public List<ReportDetailVO> detailRows(ReportQuery query) {
        return reportMapper.detailRows(query);
    }

    @Override
    public void exportWorkbook(ReportQuery query, HttpServletResponse response) {
        String dimension = dimensionOf(query);
        ReportOverviewVO overview = reportMapper.overview(query);
        List<ReportDimensionVO> dimensions = reportMapper.dimensionSummary(query, dimension);
        List<ReportDetailVO> details = reportMapper.detailRows(query);
        String filename = "统计报表_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename));
        try (Workbook workbook = reportExportService.buildWorkbook(overview, dimensions, details, dimension)) {
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException("导出统计报表失败");
        }
    }

    @Override
    public List<MonthlyReportVO> monthlySummary(ReportQuery query) {
        return reportMapper.monthlySummary(query);
    }

    @Override
    public List<CustomerReportVO> customerSummary(ReportQuery query) {
        return reportMapper.customerSummary(query);
    }

    @Override
    public List<LossReportVO> lossAnalysis(ReportQuery query) {
        return reportMapper.lossAnalysis(query);
    }

    @Override
    public List<MachineReportVO> machineOutput(ReportQuery query) {
        return reportMapper.machineOutput(query);
    }

    private String dimensionOf(ReportQuery query) {
        String dimension = query == null ? null : query.getDimension();
        if (!StringUtils.hasText(dimension)) {
            return "customer";
        }
        if (!DIMENSIONS.contains(dimension)) {
            throw new BusinessException("不支持的统计维度：" + dimension);
        }
        return dimension;
    }

    private String contentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}

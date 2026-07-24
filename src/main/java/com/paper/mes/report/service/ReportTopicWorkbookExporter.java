package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportTopicWorkbookExporter {
    private static final String[] PRODUCTION_HEADERS = {
            "维度", "加工单", "原纸吨位", "成品吨位", "损耗吨位", "损耗率%", "应收合计"
    };
    private static final String[] LOSS_HEADERS = {
            "维度", "加工单", "原纸吨位", "成品吨位", "损耗吨位", "损耗率%"
    };
    private final ReportMapper mapper;

    public SXSSFWorkbook build(String reportPath, ReportQuery query, ReportExportAuditMetadata metadata) {
        return switch (reportPath) {
            case "/reports/production" -> production(query, metadata);
            case "/reports/quality-loss" -> qualityLoss(query, metadata);
            default -> throw new BusinessException("不支持的专题报表导出路径");
        };
    }

    private SXSSFWorkbook production(ReportQuery query, ReportExportAuditMetadata metadata) {
        SXSSFWorkbook workbook = ReportWorkbookSupport.workbook();
        ReportOverviewVO overview = mapper.overview(query);
        ReportWorkbookSupport.summary(workbook, "生产总览", "生产统计总览", productionMetrics(overview));
        productionTable(workbook, "月度趋势", mapper.dimensionSummary(query, "month"), "month");
        productionTable(workbook, "工艺结构", mapper.dimensionSummary(query, "process"), "process");
        productionTable(workbook, "机台负荷",
                ReportTopicDimensionSorter.byInputWeight(mapper.dimensionSummary(query, "machine")), "machine");
        ReportWorkbookSupport.metadata(workbook, metadata);
        return workbook;
    }

    private SXSSFWorkbook qualityLoss(ReportQuery query, ReportExportAuditMetadata metadata) {
        SXSSFWorkbook workbook = ReportWorkbookSupport.workbook();
        ReportOverviewVO overview = mapper.overview(query);
        ReportWorkbookSupport.summary(workbook, "损耗总览", "质量损耗总览", lossMetrics(overview));
        lossTable(workbook, "月度趋势", mapper.dimensionSummary(query, "month"), "month");
        lossTable(workbook, "纸品损耗",
                ReportTopicDimensionSorter.byLossRisk(mapper.dimensionSummary(query, "paper")), "paper");
        lossLeaders(workbook, mapper.lossLeaderRows(query, 10));
        ReportWorkbookSupport.metadata(workbook, metadata);
        return workbook;
    }

    private void productionTable(SXSSFWorkbook workbook, String name,
                                 List<ReportDimensionVO> rows, String dimension) {
        ReportWorkbookSupport.table(workbook, name, PRODUCTION_HEADERS,
                rows.stream().map(row -> productionRow(row, dimension)).toList());
    }

    private void lossTable(SXSSFWorkbook workbook, String name,
                           List<ReportDimensionVO> rows, String dimension) {
        ReportWorkbookSupport.table(workbook, name, LOSS_HEADERS,
                rows.stream().map(row -> lossRow(row, dimension)).toList());
    }

    private void lossLeaders(SXSSFWorkbook workbook, List<ReportDetailVO> rows) {
        String[] headers = {"加工单号", "制单日期", "客户", "纸品", "工艺", "原纸吨位", "损耗吨位", "损耗率%"};
        ReportWorkbookSupport.table(workbook, "高损耗订单", headers,
                rows.stream().map(this::lossLeaderRow).toList());
    }

    private Object[] productionRow(ReportDimensionVO row, String dimension) {
        return new Object[]{ReportExportTexts.label(row, dimension), ReportWorkbookSupport.number(row.getOrderCount()),
                ReportWorkbookSupport.tons(row.getOriginalWeight()), ReportWorkbookSupport.tons(row.getFinishWeight()),
                ReportWorkbookSupport.tons(row.getLossWeight()), ReportWorkbookSupport.number(row.getLossRatio()),
                ReportWorkbookSupport.number(row.getTotalAmount())};
    }

    private Object[] lossRow(ReportDimensionVO row, String dimension) {
        return new Object[]{ReportExportTexts.label(row, dimension), ReportWorkbookSupport.number(row.getOrderCount()),
                ReportWorkbookSupport.tons(row.getOriginalWeight()), ReportWorkbookSupport.tons(row.getFinishWeight()),
                ReportWorkbookSupport.tons(row.getLossWeight()), ReportWorkbookSupport.number(row.getLossRatio())};
    }

    private Object[] lossLeaderRow(ReportDetailVO row) {
        return new Object[]{row.getOrderNo(), row.getOrderDate(), row.getCustomerName(), row.getPaperSummary(),
                row.getProcessSummary(), ReportWorkbookSupport.tons(row.getOriginalWeight()),
                ReportWorkbookSupport.tons(row.getLossWeight()), ReportWorkbookSupport.number(row.getLossRatio())};
    }

    private Object[][] productionMetrics(ReportOverviewVO row) {
        return new Object[][]{
                {"加工单数", ReportWorkbookSupport.number(row.getOrderCount()), "原卷数", ReportWorkbookSupport.number(row.getOriginalRollCount())},
                {"成品卷数", ReportWorkbookSupport.number(row.getFinishRollCount()), "刀数", ReportWorkbookSupport.number(row.getKnifeCount())},
                {"原纸吨位", ReportWorkbookSupport.tons(row.getOriginalWeight()), "成品吨位", ReportWorkbookSupport.tons(row.getFinishWeight())},
                {"损耗吨位", ReportWorkbookSupport.tons(row.getLossWeight()), "损耗率%", ReportWorkbookSupport.number(row.getLossRatio())}
        };
    }

    private Object[][] lossMetrics(ReportOverviewVO row) {
        return new Object[][]{
                {"加工单数", ReportWorkbookSupport.number(row.getOrderCount()), "原卷数", ReportWorkbookSupport.number(row.getOriginalRollCount())},
                {"原纸吨位", ReportWorkbookSupport.tons(row.getOriginalWeight()), "成品吨位", ReportWorkbookSupport.tons(row.getFinishWeight())},
                {"损耗吨位", ReportWorkbookSupport.tons(row.getLossWeight()), "损耗率%", ReportWorkbookSupport.number(row.getLossRatio())}
        };
    }
}

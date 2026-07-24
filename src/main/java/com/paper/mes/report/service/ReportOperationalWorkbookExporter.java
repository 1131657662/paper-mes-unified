package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportCollectionAnalysisVO;
import com.paper.mes.report.dto.ReportDeliveryAnalysisVO;
import com.paper.mes.report.dto.ReportExportAuditMetadata;
import com.paper.mes.report.dto.ReportInventoryAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import com.paper.mes.report.mapper.ReportOperationalMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportOperationalWorkbookExporter {
    private final ReportOperationalMapper mapper;
    private final ReportOperationalQueryPolicy queryPolicy;

    public SXSSFWorkbook build(String path, ReportQuery query, ReportExportAuditMetadata metadata) {
        return switch (path) {
            case "/reports/settlement" -> settlement(query, metadata);
            case "/reports/collection" -> collection(query, metadata);
            case "/reports/inventory" -> inventory(query, metadata);
            case "/reports/delivery" -> delivery(query, metadata);
            default -> throw new BusinessException("不支持的运营报表导出路径");
        };
    }

    private SXSSFWorkbook settlement(ReportQuery query, ReportExportAuditMetadata metadata) {
        queryPolicy.requireSettlement(query);
        SXSSFWorkbook workbook = ReportWorkbookSupport.workbook();
        settlementOverview(workbook, mapper.settlementOverview(query));
        settlementTable(workbook, "月度趋势", mapper.settlementMonthly(query));
        settlementTable(workbook, "客户应收", mapper.settlementCustomers(query));
        ReportWorkbookSupport.metadata(workbook, metadata);
        return workbook;
    }

    private SXSSFWorkbook collection(ReportQuery query, ReportExportAuditMetadata metadata) {
        queryPolicy.requireCollection(query);
        SXSSFWorkbook workbook = ReportWorkbookSupport.workbook();
        collectionOverview(workbook, mapper.collectionOverview(query));
        collectionTable(workbook, "月度趋势", mapper.collectionMonthly(query));
        collectionTable(workbook, "客户回款", mapper.collectionCustomers(query));
        ReportWorkbookSupport.metadata(workbook, metadata);
        return workbook;
    }

    private SXSSFWorkbook inventory(ReportQuery query, ReportExportAuditMetadata metadata) {
        queryPolicy.requireInventory(query);
        SXSSFWorkbook workbook = ReportWorkbookSupport.workbook();
        inventoryOverview(workbook, mapper.inventoryOverview(query));
        inventoryTable(workbook, "入库批次", mapper.inventoryMonthly(query));
        inventoryTable(workbook, "仓库分布", mapper.inventoryWarehouses(query));
        ReportWorkbookSupport.metadata(workbook, metadata);
        return workbook;
    }

    private SXSSFWorkbook delivery(ReportQuery query, ReportExportAuditMetadata metadata) {
        queryPolicy.requireDelivery(query);
        SXSSFWorkbook workbook = ReportWorkbookSupport.workbook();
        deliveryOverview(workbook, mapper.deliveryOverview(query));
        deliveryTable(workbook, "月度趋势", mapper.deliveryMonthly(query));
        deliveryTable(workbook, "仓库分布", mapper.deliveryWarehouses(query));
        ReportWorkbookSupport.metadata(workbook, metadata);
        return workbook;
    }

    private void settlementOverview(SXSSFWorkbook workbook, ReportSettlementAnalysisVO.Overview row) {
        Object[][] values = {
                {"结算单数", number(row.getTotalDocuments()), "待结算单数", number(row.getPendingDocuments())},
                {"部分结算单数", number(row.getPartialDocuments()), "逾期单数", number(row.getOverdueDocuments())},
                {"应收合计", number(row.getTotalAmount()), "已结清", number(row.getReceivedAmount())},
                {"未收金额", number(row.getUnreceivedAmount()), "逾期金额", number(row.getOverdueAmount())}
        };
        ReportWorkbookSupport.summary(workbook, "结算总览", "结算统计总览", values);
    }

    private void collectionOverview(SXSSFWorkbook workbook, ReportCollectionAnalysisVO.Overview row) {
        Object[][] values = {
                {"回款记录", number(row.getRecordCount()), "现金记录", number(row.getCashRecordCount())},
                {"废纸抵扣记录", number(row.getScrapRecordCount()), "折让记录", number(row.getDiscountRecordCount())},
                {"结清金额", number(row.getSettledAmount()), "现金到账", number(row.getCashAmount())},
                {"废纸抵扣", number(row.getScrapOffsetAmount()), "折让金额", number(row.getDiscountAmount())},
                {"废纸重量(吨)", tons(row.getScrapWeight()), "", ""}
        };
        ReportWorkbookSupport.summary(workbook, "回款总览", "回款统计总览", values);
    }

    private void inventoryOverview(SXSSFWorkbook workbook, ReportInventoryAnalysisVO.Overview row) {
        Object[][] values = {
                {"库存卷数", number(row.getRollCount()), "可用卷数", number(row.getAvailableRollCount())},
                {"锁定卷数", number(row.getLockedRollCount()), "异常卷数", number(row.getExceptionRollCount())},
                {"库存吨位", tons(row.getTotalWeight()), "锁定吨位", tons(row.getLockedWeight())}
        };
        ReportWorkbookSupport.summary(workbook, "库存总览", "库存统计总览", values);
    }

    private void deliveryOverview(SXSSFWorkbook workbook, ReportDeliveryAnalysisVO.Overview row) {
        Object[][] values = {
                {"出库单数", number(row.getDocumentCount()), "待出库单数", number(row.getPendingDocuments())},
                {"完成单数", number(row.getCompletedDocuments()), "出库卷数", number(row.getRollCount())},
                {"出库吨位", tons(row.getTotalWeight()), "待出库吨位", tons(row.getPendingWeight())},
                {"完成吨位", tons(row.getCompletedWeight()), "", ""}
        };
        ReportWorkbookSupport.summary(workbook, "出库总览", "出库统计总览", values);
    }

    private void settlementTable(SXSSFWorkbook workbook, String name,
                                 List<ReportSettlementAnalysisVO.Dimension> rows) {
        String[] headers = {"维度", "单据数", "应收合计", "已结清", "未收金额"};
        ReportWorkbookSupport.table(workbook, name, headers, rows.stream().map(row -> new Object[]{
                label(row.getDimensionName(), row.getDimensionKey()), number(row.getDocumentCount()),
                number(row.getTotalAmount()), number(row.getReceivedAmount()), number(row.getUnreceivedAmount())
        }).toList());
    }

    private void collectionTable(SXSSFWorkbook workbook, String name,
                                 List<ReportCollectionAnalysisVO.Dimension> rows) {
        String[] headers = {"维度", "记录数", "结清金额", "现金到账", "非现金金额", "废纸重量(吨)"};
        ReportWorkbookSupport.table(workbook, name, headers, rows.stream().map(row -> new Object[]{
                label(row.getDimensionName(), row.getDimensionKey()), number(row.getRecordCount()),
                number(row.getSettledAmount()), number(row.getCashAmount()), number(row.getNonCashAmount()),
                tons(row.getScrapWeight())
        }).toList());
    }

    private void inventoryTable(SXSSFWorkbook workbook, String name,
                                List<ReportInventoryAnalysisVO.Dimension> rows) {
        String[] headers = {"维度", "库存卷数", "库存吨位", "锁定吨位"};
        ReportWorkbookSupport.table(workbook, name, headers, rows.stream().map(row -> new Object[]{
                label(row.getDimensionName(), row.getDimensionKey()), number(row.getRollCount()),
                tons(row.getTotalWeight()), tons(row.getLockedWeight())
        }).toList());
    }

    private void deliveryTable(SXSSFWorkbook workbook, String name,
                               List<ReportDeliveryAnalysisVO.Dimension> rows) {
        String[] headers = {"维度", "出库单数", "卷数", "出库吨位", "完成吨位"};
        ReportWorkbookSupport.table(workbook, name, headers, rows.stream().map(row -> new Object[]{
                label(row.getDimensionName(), row.getDimensionKey()), number(row.getDocumentCount()),
                number(row.getRollCount()), tons(row.getTotalWeight()), tons(row.getCompletedWeight())
        }).toList());
    }

    private double number(Number value) {
        return ReportWorkbookSupport.number(value);
    }

    private double tons(java.math.BigDecimal value) {
        return ReportWorkbookSupport.tons(value);
    }

    private String label(String name, String key) {
        return name == null || name.isBlank() ? key : name;
    }
}

package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettleExportServiceTest {

    @Test
    void buildWorkbook_whenExportingSettle_includesOriginalRollAndFeeBreakdown() {
        SettleExportService service = new SettleExportService();

        Workbook workbook = service.buildWorkbook(detail());

        var sheet = workbook.getSheetAt(0);
        assertEquals("原纸", text(sheet.getRow(7).getCell(3)));
        assertEquals("加工内容", text(sheet.getRow(7).getCell(8)));
        assertEquals("成品摘要", text(sheet.getRow(7).getCell(9)));
        assertEquals("锯纸单价", text(sheet.getRow(7).getCell(13)));
        assertEquals("额外费说明", text(sheet.getRow(7).getCell(19)));
        assertEquals("开票", text(sheet.getRow(7).getCell(20)));
        assertEquals("应收合计", text(sheet.getRow(7).getCell(21)));
        assertEquals("母卷1", text(sheet.getRow(8).getCell(3)));
        assertEquals("锯纸+复卷", text(sheet.getRow(8).getCell(8)));
        assertEquals("A000001、A000002", text(sheet.getRow(8).getCell(9)));
        assertEquals("200（开票价 212）", text(sheet.getRow(8).getCell(13)));
        assertEquals("装卸费 80.00；运费 30.00", text(sheet.getRow(8).getCell(19)));
        assertEquals("开票", text(sheet.getRow(8).getCell(20)));
        assertEquals("1176", text(sheet.getRow(8).getCell(21)));
    }

    @Test
    void buildWorkbook_whenExportingSettle_includesReceiveSheetWithCancelledRecords() {
        SettleExportService service = new SettleExportService();

        Workbook workbook = service.buildWorkbook(detail());

        var billSheet = workbook.getSheet("结算单");
        assertEquals("已收金额", text(billSheet.getRow(4).getCell(3)));
        assertEquals("500", text(billSheet.getRow(4).getCell(4)));
        assertEquals("未收金额", text(billSheet.getRow(5).getCell(0)));
        assertEquals("676", text(billSheet.getRow(5).getCell(1)));

        var receiveSheet = workbook.getSheet("收款流水");
        assertEquals("收款流水", text(receiveSheet.getRow(0).getCell(0)));
        assertEquals("收款金额", text(receiveSheet.getRow(3).getCell(2)));
        assertEquals("状态", text(receiveSheet.getRow(3).getCell(6)));
        assertEquals("500", text(receiveSheet.getRow(4).getCell(2)));
        assertEquals("转账", text(receiveSheet.getRow(4).getCell(3)));
        assertEquals("有效", text(receiveSheet.getRow(4).getCell(6)));
        assertEquals("100", text(receiveSheet.getRow(5).getCell(2)));
        assertEquals("已撤销", text(receiveSheet.getRow(5).getCell(6)));
        assertEquals("客户重复付款", text(receiveSheet.getRow(5).getCell(9)));
    }

    private SettleDetailVO detail() {
        SettleDetailVO vo = new SettleDetailVO();
        SettleOrder order = new SettleOrder();
        order.setSettleNo("JS202607010001");
        order.setCustomerName("测试客户");
        order.setSettleDate(LocalDate.of(2026, 7, 1));
        order.setIsInvoice(1);
        order.setSettleStatus(1);
        order.setTotalAmount(new BigDecimal("1176"));
        order.setReceivedAmount(new BigDecimal("500"));
        order.setUnreceivedAmount(new BigDecimal("676"));
        vo.setOrder(order);
        vo.setPrintLines(List.of(line()));
        vo.setReceives(List.of(activeReceive(), cancelledReceive()));
        return vo;
    }

    private SettlePrintLineVO line() {
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setOrderUuid("order-1");
        line.setOrderNo("JG202607010001");
        line.setOrderDate(LocalDate.of(2026, 7, 1));
        line.setOriginalLabel("母卷1");
        line.setPaperName("牛卡纸");
        line.setGramWeight(450);
        line.setOriginalWidth(2500);
        line.setOriginalWeight(new BigDecimal("3255"));
        line.setProcessText("锯纸+复卷");
        line.setFinishSummary("A000001、A000002");
        line.setFinishCount(2);
        line.setFinishWeight(new BigDecimal("3000"));
        line.setTrimWeight(new BigDecimal("55"));
        line.setSawUnitPrice(new BigDecimal("200"));
        line.setSawInvoiceUnitPrice(new BigDecimal("212.00"));
        line.setSawAmount(new BigDecimal("651"));
        line.setRewindUnitPrice(new BigDecimal("150"));
        line.setRewindAmount(new BigDecimal("359"));
        line.setProcessAmount(new BigDecimal("1010"));
        line.setExtraAmount(new BigDecimal("100"));
        line.setExtraFeeSummary("装卸费 80.00；运费 30.00");
        line.setTaxAmount(new BigDecimal("66"));
        line.setIsInvoice(1);
        line.setLineAmount(new BigDecimal("1176"));
        return line;
    }

    private ReceiveRecord activeReceive() {
        ReceiveRecord record = new ReceiveRecord();
        record.setReceiveDate(LocalDateTime.of(2026, 7, 2, 9, 30));
        record.setReceiveAmount(new BigDecimal("500"));
        record.setPayMethod(2);
        record.setPayNo("PAY001");
        record.setOperator("财务");
        record.setRecordStatus(1);
        record.setRemark("首款");
        return record;
    }

    private ReceiveRecord cancelledReceive() {
        ReceiveRecord record = new ReceiveRecord();
        record.setReceiveDate(LocalDateTime.of(2026, 7, 2, 10, 30));
        record.setReceiveAmount(new BigDecimal("100"));
        record.setPayMethod(1);
        record.setOperator("财务");
        record.setRecordStatus(2);
        record.setCancelBy("主管");
        record.setCancelReason("客户重复付款");
        return record;
    }

    private String text(org.apache.poi.ss.usermodel.Cell cell) {
        return cell == null ? "" : cell.toString();
    }
}

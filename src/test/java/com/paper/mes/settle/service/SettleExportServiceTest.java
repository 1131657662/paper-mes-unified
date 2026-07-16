package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettleExportServiceTest {

    @Test
    void buildWorkbook_whenExportingSettle_usesCustomerFacingColumnsAndGroupedFees() throws IOException {
        SettleExportService service = new SettleExportService();

        try (Workbook workbook = service.buildWorkbook(detail())) {
            var sheet = workbook.getSheetAt(0);

            assertEquals(8, sheet.getRow(9).getPhysicalNumberOfCells());
            assertEquals("原纸", text(sheet.getRow(9).getCell(0)));
            assertEquals("加工项目", text(sheet.getRow(9).getCell(3)));
            assertEquals("计费依据", text(sheet.getRow(9).getCell(4)));
            assertEquals("加工费", text(sheet.getRow(9).getCell(7)));
            assertEquals("JG202607010001", text(sheet.getRow(10).getCell(1)));
            assertEquals("母卷1", text(sheet.getRow(11).getCell(0)));
            assertEquals("牛卡纸 / 450 g / 2500 mm", text(sheet.getRow(11).getCell(1)));
            assertEquals("锯纸 / 复卷", text(sheet.getRow(11).getCell(3)));
            assertEquals("6刀 × 200元/刀 = 651；2t × 150元/t = 359", text(sheet.getRow(11).getCell(4)));
            assertTrue(sheet.getRow(11).getCell(4).getCellStyle().getWrapText());
            assertEquals("2 卷 / 3000 kg（A000001、A000002）", text(sheet.getRow(11).getCell(5)));
            assertEquals("JG202607010001 小计", text(sheet.getRow(12).getCell(0)));
            assertEquals("100（装卸费 80.00；运费 30.00）", text(sheet.getRow(13).getCell(4)));
            assertEquals("税费 66", text(sheet.getRow(13).getCell(5)));
            assertEquals("1176", text(sheet.getRow(13).getCell(7)));
        }
    }

    @Test
    void buildWorkbook_whenLegacySnapshotHasNoTaxAmount_derivesGroupTax() throws IOException {
        SettleDetailVO detail = detail();
        detail.getPrintLines().getFirst().setTaxAmount(null);

        try (Workbook workbook = new SettleExportService().buildWorkbook(detail)) {
            assertEquals("税费 66", text(workbook.getSheetAt(0).getRow(13).getCell(5)));
        }
    }

    @Test
    void buildWorkbook_whenExportingSettle_includesReceiveSheetWithScrapOffset() throws IOException {
        SettleExportService service = new SettleExportService();

        try (Workbook workbook = service.buildWorkbook(detail())) {
            var billSheet = workbook.getSheet("结算单");
            assertEquals("已结清", text(billSheet.getRow(4).getCell(3)));
            assertEquals("500", text(billSheet.getRow(4).getCell(4)));
            assertEquals("现金实收", text(billSheet.getRow(5).getCell(0)));
            assertEquals("400", text(billSheet.getRow(5).getCell(1)));
            assertEquals("废纸抵扣", text(billSheet.getRow(5).getCell(3)));
            assertEquals("90", text(billSheet.getRow(5).getCell(4)));
            assertEquals("优惠核销", text(billSheet.getRow(6).getCell(0)));
            assertEquals("10", text(billSheet.getRow(6).getCell(1)));
            assertEquals("未收金额", text(billSheet.getRow(6).getCell(3)));
            assertEquals("676", text(billSheet.getRow(6).getCell(4)));

            var receiveSheet = workbook.getSheet("收款流水");
            assertEquals("收款流水", text(receiveSheet.getRow(0).getCell(0)));
            assertEquals("类型", text(receiveSheet.getRow(3).getCell(2)));
            assertEquals("本次结清", text(receiveSheet.getRow(3).getCell(3)));
            assertEquals("现金实收", text(receiveSheet.getRow(3).getCell(4)));
            assertEquals("废纸抵扣", text(receiveSheet.getRow(3).getCell(5)));
            assertEquals("优惠核销", text(receiveSheet.getRow(3).getCell(6)));
            assertEquals("状态", text(receiveSheet.getRow(3).getCell(12)));
            assertEquals("混合结清", text(receiveSheet.getRow(4).getCell(2)));
            assertEquals("500", text(receiveSheet.getRow(4).getCell(3)));
            assertEquals("400", text(receiveSheet.getRow(4).getCell(4)));
            assertEquals("90", text(receiveSheet.getRow(4).getCell(5)));
            assertEquals("10", text(receiveSheet.getRow(4).getCell(6)));
            assertEquals("45", text(receiveSheet.getRow(4).getCell(7)));
            assertEquals("2", text(receiveSheet.getRow(4).getCell(8)));
            assertEquals("转账", text(receiveSheet.getRow(4).getCell(9)));
            assertEquals("有效", text(receiveSheet.getRow(4).getCell(12)));
            assertEquals("已撤销", text(receiveSheet.getRow(5).getCell(12)));
            assertEquals("主管 / 客户重复付款", text(receiveSheet.getRow(5).getCell(14)));
        }
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
        order.setCashReceivedAmount(new BigDecimal("400"));
        order.setScrapOffsetAmount(new BigDecimal("90"));
        order.setDiscountAmount(new BigDecimal("10"));
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
        line.setFeeLines(List.of(sawFee(), rewindFee()));
        return line;
    }

    private SettleFeeLineVO sawFee() {
        SettleFeeLineVO fee = new SettleFeeLineVO();
        fee.setFeeType("saw");
        fee.setFeeName("锯纸费");
        fee.setFormulaText("6刀 × 200元/刀");
        fee.setAmountNoTax(new BigDecimal("651"));
        return fee;
    }

    private SettleFeeLineVO rewindFee() {
        SettleFeeLineVO fee = new SettleFeeLineVO();
        fee.setFeeType("rewind");
        fee.setFeeName("复卷费");
        fee.setFormulaText("2t × 150元/t");
        fee.setAmountNoTax(new BigDecimal("359"));
        return fee;
    }

    private ReceiveRecord activeReceive() {
        ReceiveRecord record = new ReceiveRecord();
        record.setReceiveDate(LocalDateTime.of(2026, 7, 2, 9, 30));
        record.setReceiveAmount(new BigDecimal("500"));
        record.setCashAmount(new BigDecimal("400"));
        record.setScrapOffsetAmount(new BigDecimal("90"));
        record.setDiscountAmount(new BigDecimal("10"));
        record.setScrapWeight(new BigDecimal("45"));
        record.setScrapUnitPrice(new BigDecimal("2"));
        record.setReceiveType(3);
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
        record.setCashAmount(new BigDecimal("100"));
        record.setReceiveType(1);
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

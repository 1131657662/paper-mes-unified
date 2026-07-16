package com.paper.mes.safety;

import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.service.DeliveryExportService;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.service.SettleExportService;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FullFlowDemoDataAcceptanceTest {

    @Test
    void demoData_whenRunningProcessToDeliverySettleReceive_coversMainBusinessScenarios() throws IOException {
        DeliveryDetailVO delivery = deliveryDetail();
        SettleDetailVO settle = settleDetail();

        try (Workbook deliveryWorkbook = new DeliveryExportService().buildWorkbook(delivery);
             Workbook settleWorkbook = new SettleExportService().buildWorkbook(settle)) {
            assertEquals(5, delivery.getDetails().size());
            assertEquals("CK-DEMO-0001", text(deliveryWorkbook.getSheetAt(0).getRow(1).getCell(1)));
            assertEquals("JG-DEMO-0001", text(deliveryWorkbook.getSheetAt(0).getRow(9).getCell(1)));
            assertEquals("JG-DEMO-0002", text(deliveryWorkbook.getSheetAt(0).getRow(12).getCell(1)));
            assertEquals("直发", text(deliveryWorkbook.getSheetAt(0).getRow(12).getCell(9)));

            assertEquals(5, settle.getPrintLines().size());
            assertEquals("JS-DEMO-0001", text(settleWorkbook.getSheetAt(0).getRow(1).getCell(1)));
            assertEquals("锯纸 + 现场定尺", text(settleWorkbook.getSheetAt(0).getRow(11).getCell(3)));
            assertEquals("直发", text(settleWorkbook.getSheetAt(0).getRow(17).getCell(3)));
            assertEquals("250（装卸费 120.00；运费 80.00；其他费 50.00）",
                    text(settleWorkbook.getSheetAt(0).getRow(15).getCell(4)));
            assertEquals("有效", text(settleWorkbook.getSheet("收款流水").getRow(4).getCell(12)));
            assertEquals("已撤销", text(settleWorkbook.getSheet("收款流水").getRow(5).getCell(12)));
        }
    }

    private DeliveryDetailVO deliveryDetail() {
        DeliveryDetailVO vo = new DeliveryDetailVO();
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryNo("CK-DEMO-0001");
        order.setCustomerName("全链路模拟客户");
        order.setDeliveryDate(LocalDate.of(2026, 7, 2));
        order.setDeliveryStatus(2);
        order.setTotalCount(5);
        order.setTotalWeight(new BigDecimal("10680.000"));
        vo.setOrder(order);
        vo.setDetails(List.of(
                deliveryItem("JG-DEMO-0001", "A-DEMO-001", "牛卡纸", 450, 780, "锯纸 + 现场定尺",
                        "锯纸 4刀 / 定尺 780mm", "母卷1 / 卷号R-D001 / 2500mm / 3255.000kg", 2010),
                deliveryItem("JG-DEMO-0001", "A-DEMO-002", "牛卡纸", 450, 780, "锯纸 + 现场定尺",
                        "锯纸 4刀 / 定尺 780mm", "母卷1 / 卷号R-D001 / 2500mm / 3255.000kg", 1988),
                deliveryItem("JG-DEMO-0001", "A-DEMO-003", "牛卡纸", 450, 950, "复卷",
                        "复卷 950mm x 2", "母卷2 / 卷号R-D002 / 2500mm / 3200.000kg", 2200),
                deliveryItem("JG-DEMO-0002", "A-DEMO-004", "白卡纸", 300, 2000, "直发",
                        "直发原纸出库", "母卷1 / 卷号R-D003 / 2000mm / 2202.000kg", 2202),
                deliveryItem("JG-DEMO-0002", "A-DEMO-005", "白卡纸", 300, 1000, "复卷",
                        "复卷 1000mm x 2", "母卷2 / 卷号R-D004 / 2000mm / 2280.000kg", 2280)
        ));
        return vo;
    }

    private DeliveryDetailItemVO deliveryItem(String orderNo, String finishNo, String paperName, int gram,
                                              int width, String mode, String process, String original, int weight) {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setOrderNo(orderNo);
        item.setFinishRollNo(finishNo);
        item.setPaperName(paperName);
        item.setGramWeight(gram);
        item.setFinishWidth(width);
        item.setActualWeight(new BigDecimal(weight));
        item.setOutWeight(new BigDecimal(weight));
        item.setOriginalSummary(original);
        item.setProcessModeText(mode);
        item.setProcessSummary(process);
        item.setSourceType("直发".equals(mode) ? 2 : 1);
        item.setFinishStatus(3);
        return item;
    }

    private SettleDetailVO settleDetail() {
        SettleDetailVO vo = new SettleDetailVO();
        SettleOrder order = new SettleOrder();
        order.setSettleNo("JS-DEMO-0001");
        order.setCustomerName("全链路模拟客户");
        order.setSettleDate(LocalDate.of(2026, 7, 3));
        order.setIsInvoice(1);
        order.setSettleStatus(2);
        order.setSawAmount(new BigDecimal("1302.00"));
        order.setRewindAmount(new BigDecimal("1200.00"));
        order.setExtraAmount(new BigDecimal("250.00"));
        order.setTotalAmount(new BigDecimal("2917.12"));
        order.setReceivedAmount(new BigDecimal("1500.00"));
        order.setCashReceivedAmount(new BigDecimal("1500.00"));
        order.setScrapOffsetAmount(BigDecimal.ZERO);
        order.setUnreceivedAmount(new BigDecimal("1417.12"));
        vo.setOrder(order);
        vo.setPrintLines(List.of(
                settleLine("JG-DEMO-0001", "母卷1", "牛卡纸", "锯纸 + 现场定尺", "A-DEMO-001、A-DEMO-002",
                        "锯纸 4刀 / 单价 200.00", "装卸费 120.00；运费 80.00；其他费 50.00", 651, 0, 125, 823.56),
                settleLine("JG-DEMO-0001", "母卷2", "牛卡纸", "复卷", "A-DEMO-003",
                        "复卷 3.200t / 单价 150.00", "装卸费 120.00；运费 80.00；其他费 50.00", 0, 480, 125, 641.30),
                settleLine("JG-DEMO-0001", "母卷3", "牛卡纸", "锯纸 + 复卷", "A-DEMO-006",
                        "锯纸 2刀 / 复卷 1.600t", "装卸费 120.00；运费 80.00；其他费 50.00", 651, 240, 0, 944.46),
                settleLine("JG-DEMO-0002", "母卷1", "白卡纸", "直发", "A-DEMO-004",
                        "直发不计加工费", null, 0, 0, 0, 0),
                settleLine("JG-DEMO-0002", "母卷2", "白卡纸", "复卷", "A-DEMO-005",
                        "复卷 2.280t / 单价 150.00", null, 0, 480, 0, 507.80)
        ));
        vo.setReceives(List.of(activeReceive(), cancelledReceive()));
        return vo;
    }

    private SettlePrintLineVO settleLine(String orderNo, String originalLabel, String paperName, String process,
                                         String finishes, String processSummary, String extraSummary,
                                         double saw, double rewind, double extra, double total) {
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setOrderUuid(orderNo);
        line.setOrderNo(orderNo);
        line.setOrderDate(LocalDate.of(2026, 7, "JG-DEMO-0001".equals(orderNo) ? 1 : 2));
        line.setOriginalLabel(originalLabel);
        line.setOriginalRollNo("R-" + originalLabel.replace("母卷", "D00"));
        line.setPaperName(paperName);
        line.setGramWeight("牛卡纸".equals(paperName) ? 450 : 300);
        line.setOriginalWidth("牛卡纸".equals(paperName) ? 2500 : 2000);
        line.setOriginalWeight(new BigDecimal("3200.000"));
        line.setProcessText(process);
        line.setProcessStepSummary(processSummary);
        line.setFinishSummary(finishes);
        line.setFinishDetailSummary(finishes + " / 明细已冻结");
        line.setFinishCount(finishes.split("、").length);
        line.setFinishWeight(new BigDecimal("2100.000"));
        line.setTrimWeight(new BigDecimal("35.000"));
        line.setTrimSummary("修边 35.000kg");
        line.setSawUnitPrice(new BigDecimal("200.00"));
        line.setSawInvoiceUnitPrice(new BigDecimal("212.00"));
        line.setRewindUnitPrice(new BigDecimal("150.00"));
        line.setRewindInvoiceUnitPrice(new BigDecimal("159.00"));
        line.setSawAmount(BigDecimal.valueOf(saw));
        line.setRewindAmount(BigDecimal.valueOf(rewind));
        line.setProcessAmount(BigDecimal.valueOf(saw + rewind));
        line.setExtraAmount(BigDecimal.valueOf(extra));
        line.setExtraFeeSummary(extraSummary);
        line.setTaxRate(new BigDecimal("6.00"));
        line.setTaxAmount(BigDecimal.valueOf(total - saw - rewind - extra));
        line.setIsInvoice(1);
        line.setLineAmount(BigDecimal.valueOf(total));
        return line;
    }

    private ReceiveRecord activeReceive() {
        ReceiveRecord record = new ReceiveRecord();
        record.setReceiveDate(LocalDateTime.of(2026, 7, 4, 9, 0));
        record.setReceiveAmount(new BigDecimal("1500.00"));
        record.setCashAmount(new BigDecimal("1500.00"));
        record.setReceiveType(1);
        record.setPayMethod(2);
        record.setOperator("财务");
        record.setRecordStatus(1);
        return record;
    }

    private ReceiveRecord cancelledReceive() {
        ReceiveRecord record = new ReceiveRecord();
        record.setReceiveDate(LocalDateTime.of(2026, 7, 4, 10, 0));
        record.setReceiveAmount(new BigDecimal("300.00"));
        record.setCashAmount(new BigDecimal("300.00"));
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

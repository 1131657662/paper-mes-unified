package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryExportServiceTest {

    @Test
    void buildWorkbook_whenExportingDelivery_includesTraceableDetailColumns() {
        DeliveryExportService service = new DeliveryExportService();

        Workbook workbook = service.buildWorkbook(detail());

        var sheet = workbook.getSheetAt(0);
        assertEquals("卷号", text(sheet.getRow(8).getCell(2)));
        assertEquals("品名", text(sheet.getRow(8).getCell(3)));
        assertEquals("克重", text(sheet.getRow(8).getCell(4)));
        assertEquals("规格", text(sheet.getRow(8).getCell(5)));
        assertEquals("件重kg", text(sheet.getRow(8).getCell(6)));
        assertEquals("原纸信息", text(sheet.getRow(8).getCell(8)));
        assertEquals("工艺摘要", text(sheet.getRow(8).getCell(10)));
        assertEquals("回录备注", text(sheet.getRow(8).getCell(14)));
        assertEquals("A000001", text(sheet.getRow(9).getCell(2)));
        assertEquals("白卡纸", text(sheet.getRow(9).getCell(3)));
        assertEquals("母卷1 / 2500mm / 3255kg", text(sheet.getRow(9).getCell(8)));
        assertEquals("复卷 950×2 + 修边", text(sheet.getRow(9).getCell(10)));
    }

    private DeliveryDetailVO detail() {
        DeliveryDetailVO vo = new DeliveryDetailVO();
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryNo("CK202607010001");
        order.setCustomerName("测试客户");
        order.setDeliveryDate(LocalDate.of(2026, 7, 1));
        order.setDeliveryStatus(2);
        order.setTotalCount(1);
        order.setTotalWeight(new BigDecimal("1200"));
        vo.setOrder(order);
        vo.setDetails(List.of(item()));
        return vo;
    }

    private DeliveryDetailItemVO item() {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setOrderNo("JG202607010001");
        item.setFinishRollNo("A000001");
        item.setPaperName("白卡纸");
        item.setGramWeight(300);
        item.setFinishWidth(950);
        item.setFinishDiameter(1200);
        item.setActualWeight(new BigDecimal("1200"));
        item.setOutWeight(new BigDecimal("1198"));
        item.setOriginalSummary("母卷1 / 2500mm / 3255kg");
        item.setProcessModeText("复卷");
        item.setProcessSummary("复卷 950×2 + 修边");
        item.setSourceType(1);
        item.setFinishStatus(3);
        item.setRemark("客户指定批次");
        item.setActualRemark("实磅正常");
        return item;
    }

    private String text(org.apache.poi.ss.usermodel.Cell cell) {
        return cell == null ? "" : cell.toString();
    }
}

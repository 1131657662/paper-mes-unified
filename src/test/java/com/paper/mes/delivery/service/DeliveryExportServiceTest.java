package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
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
    void buildWorkbook_whenExportingDelivery_includesTraceableDetailColumns() throws Exception {
        DeliveryExportService service = new DeliveryExportService();

        try (Workbook workbook = service.buildWorkbook(detail())) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("卷号", text(sheet.getRow(8).getCell(2)));
            assertEquals("实物品名", text(sheet.getRow(8).getCell(3)));
            assertEquals("实物克重", text(sheet.getRow(8).getCell(4)));
            assertEquals("实物规格", text(sheet.getRow(8).getCell(5)));
            assertEquals("实物件重kg", text(sheet.getRow(8).getCell(6)));
            assertEquals("原纸信息", text(sheet.getRow(8).getCell(8)));
            assertEquals("工艺摘要", text(sheet.getRow(8).getCell(10)));
            assertEquals("回录备注", text(sheet.getRow(8).getCell(14)));
            assertEquals("A000001", text(sheet.getRow(9).getCell(2)));
            assertEquals("白卡纸", text(sheet.getRow(9).getCell(3)));
            assertEquals("母卷1 / 2500mm / 3255kg", text(sheet.getRow(9).getCell(8)));
            assertEquals("复卷 950×2 + 修边", text(sheet.getRow(9).getCell(10)));
        }
    }

    @Test
    void buildWorkbook_whenCustomerRevisionExists_exportsCustomerDocumentSheet() throws Exception {
        DeliveryExportService service = new DeliveryExportService();

        try (Workbook workbook = service.buildWorkbook(detail(), customerSpecs())) {
            var sheet = workbook.getSheet("客户单据");

            assertEquals("V2", text(sheet.getRow(2).getCell(1)));
            assertEquals("客户品名", text(sheet.getRow(7).getCell(7)));
            assertEquals("食品卡", text(sheet.getRow(8).getCell(7)));
            assertEquals("275", text(sheet.getRow(8).getCell(8)));
            assertEquals("1205", text(sheet.getRow(8).getCell(9)));
            assertEquals("1299", text(sheet.getRow(8).getCell(10)));
            assertEquals("101", text(sheet.getRow(8).getCell(11)));
            assertEquals("出库客户更正版", text(sheet.getRow(8).getCell(13)));
        }
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

    private DeliveryCustomerRevisionPreviewVO customerSpecs() {
        DeliveryCustomerSpecVO item = new DeliveryCustomerSpecVO();
        item.setDeliveryDetailUuid("detail-1");
        item.setOrderNo("JG202607010001");
        item.setFinishRollNo("A000001");
        item.setPhysicalPaperName("白卡纸");
        item.setPhysicalGramWeight(300);
        item.setPhysicalFinishWidth(950);
        item.setPhysicalDeliveryWeight(new BigDecimal("1198"));
        item.setCustomerPaperName("食品卡");
        item.setCustomerGramWeight(275);
        item.setCustomerFinishWidth(1205);
        item.setCustomerDisplayWeight(new BigDecimal("1299"));
        item.setSpecificationChanged(true);
        item.setWeightChanged(true);
        item.setValueSource("DELIVERY_REVISION");
        DeliveryCustomerRevisionPreviewVO preview = new DeliveryCustomerRevisionPreviewVO();
        preview.setCurrentRevisionNo(2);
        preview.setCurrentRevisionKind(DeliveryCustomerRevisionPreviewService.REVISION_KIND_USER);
        preview.setPhysicalTotalWeight(new BigDecimal("1198"));
        preview.setCustomerTotalWeight(new BigDecimal("1299"));
        preview.setDifferenceWeight(new BigDecimal("101"));
        preview.setItems(List.of(item));
        return preview;
    }

    private String text(org.apache.poi.ss.usermodel.Cell cell) {
        return cell == null ? "" : cell.toString();
    }
}

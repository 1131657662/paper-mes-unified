package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryExportOriginalColumnsTest {

    @Test
    void buildWorkbook_withMultipleOriginals_exportsAlignedOriginalColumns() throws Exception {
        DeliveryDetailItemVO item = itemWithSources("detail-1", "finish-1",
                List.of(secondSource(), firstSource()));

        try (Workbook workbook = reopen(new DeliveryExportService().buildWorkbook(detail(List.of(item))))) {
            Row header = workbook.getSheet("出库单").getRow(8);
            Row values = workbook.getSheet("出库单").getRow(9);
            assertArrayEquals(originalHeaders(), cells(header, 13, 16));
            assertArrayEquals(originalValues(), cells(values, 13, 16));
            assertEquals(35, header.getLastCellNum());
        }
    }

    @Test
    void buildWorkbook_forCustomerSheet_matchesOriginalsByDeliveryDetailUuidFirst() throws Exception {
        DeliveryDetailItemVO decoy = itemWithSources("detail-decoy", "finish-decoy",
                List.of(minimalSource("R-DECOY")));
        DeliveryDetailItemVO target = itemWithSources("detail-target", "finish-target",
                List.of(minimalSource("R-TARGET")));

        try (Workbook workbook = new DeliveryExportService().buildWorkbook(
                detail(List.of(decoy, target)), customerSpecs())) {
            Row header = workbook.getSheet("客户单据").getRow(7);
            Row values = workbook.getSheet("客户单据").getRow(8);
            assertArrayEquals(originalHeaders(), cells(header, 15, 16));
            assertEquals("R-TARGET", values.getCell(16).toString());
            assertEquals(31, header.getLastCellNum());
        }
    }

    private String[] originalHeaders() {
        return new String[]{"母卷序号", "母卷卷号", "母卷编号", "母卷品名", "母卷标称克重", "母卷实际克重",
                "母卷标称门幅", "母卷实际门幅", "母卷标称重量kg", "母卷实际重量kg", "母卷加工方式",
                "母卷主工艺", "母卷机台", "母卷操作员", "母卷备注", "历史原纸信息"};
    }

    private String[] originalValues() {
        return new String[]{"1\n2", "R-1\nR-2", "E-1\nE-2", "牛卡纸\n白卡纸", "250\n300", "252\n302",
                "2400\n2500", "2398\n2495", "3100.5\n3200", "3088.25\n3180.75",
                "标准加工\n现场定尺", "锯纸\n复卷", "一号机\n二号机", "张三\n李四", "首卷\n次卷", "-"};
    }

    private String[] cells(Row row, int start, int count) {
        String[] values = new String[count];
        for (int index = 0; index < count; index++) values[index] = row.getCell(start + index).toString();
        return values;
    }

    private Workbook reopen(Workbook source) throws IOException {
        try (source; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            source.write(output);
            return WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()));
        }
    }

    private DeliveryDetailVO detail(List<DeliveryDetailItemVO> items) {
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryNo("CK-EXPORT-1");
        order.setCustomerName("测试客户");
        DeliveryDetailVO detail = new DeliveryDetailVO();
        detail.setOrder(order);
        detail.setDetails(items);
        return detail;
    }

    private DeliveryDetailItemVO itemWithSources(String uuid, String finishUuid,
                                                 List<DeliveryDetailItemVO.OriginalSourceItem> sources) {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setUuid(uuid);
        item.setFinishUuid(finishUuid);
        item.setOrderNo("JG-EXPORT-1");
        item.setFinishRollNo("A-EXPORT-1");
        item.setOriginalItems(sources);
        return item;
    }

    private DeliveryDetailItemVO.OriginalSourceItem firstSource() {
        DeliveryDetailItemVO.OriginalSourceItem source = minimalSource("R-1");
        source.setRowSort(1);
        source.setExtraNo("E-1");
        source.setPaperName("牛卡纸");
        source.setGramWeight(250);
        source.setActualGramWeight(252);
        source.setOriginalWidth(2400);
        source.setActualWidth(2398);
        source.setTotalWeight(new BigDecimal("3100.500"));
        source.setActualWeight(new BigDecimal("3088.250"));
        source.setProcessMode(1);
        source.setMainStepType(1);
        source.setMachineName("一号机");
        source.setOperator("张三");
        source.setRemark("首卷");
        return source;
    }

    private DeliveryDetailItemVO.OriginalSourceItem secondSource() {
        DeliveryDetailItemVO.OriginalSourceItem source = minimalSource("R-2");
        source.setRowSort(2);
        source.setExtraNo("E-2");
        source.setPaperName("白卡纸");
        source.setGramWeight(300);
        source.setActualGramWeight(302);
        source.setOriginalWidth(2500);
        source.setActualWidth(2495);
        source.setTotalWeight(new BigDecimal("3200"));
        source.setActualWeight(new BigDecimal("3180.75"));
        source.setProcessMode(2);
        source.setMainStepType(2);
        source.setMachineName("二号机");
        source.setOperator("李四");
        source.setRemark("次卷");
        return source;
    }

    private DeliveryDetailItemVO.OriginalSourceItem minimalSource(String rollNo) {
        DeliveryDetailItemVO.OriginalSourceItem source = new DeliveryDetailItemVO.OriginalSourceItem();
        source.setRowSort(1);
        source.setRollNo(rollNo);
        return source;
    }

    private DeliveryCustomerRevisionPreviewVO customerSpecs() {
        DeliveryCustomerSpecVO item = new DeliveryCustomerSpecVO();
        item.setDeliveryDetailUuid("detail-target");
        item.setFinishUuid("finish-decoy");
        item.setOrderNo("JG-EXPORT-1");
        item.setFinishRollNo("A-EXPORT-1");
        DeliveryCustomerRevisionPreviewVO preview = new DeliveryCustomerRevisionPreviewVO();
        preview.setCurrentRevisionNo(1);
        preview.setItems(List.of(item));
        return preview;
    }
}

package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;


import static com.paper.mes.delivery.service.DeliveryExportText.*;

/**
 * 出库单 Excel 导出装配。
 */
@Service
public class DeliveryExportService {

    public Workbook buildWorkbook(DeliveryDetailVO detail) {
        return buildWorkbook(detail, null);
    }

    public Workbook buildWorkbook(DeliveryDetailVO detail, DeliveryCustomerRevisionPreviewVO customerSpecs) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("出库单");
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle headerStyle = headerStyle(workbook);
        writeSummary(sheet, detail.getOrder(), titleStyle);
        writeHeader(sheet, headerStyle);
        writeItems(sheet, detail);
        autosize(sheet, 15);
        if (customerSpecs != null) {
            DeliveryCustomerExportWriter.write(workbook.createSheet("客户单据"), detail, customerSpecs);
        }
        return workbook;
    }

    private void writeSummary(Sheet sheet, DeliveryOrder order, CellStyle titleStyle) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("出库单明细");
        title.getCell(0).setCellStyle(titleStyle);
        row(sheet, 1, "出库单号", order.getDeliveryNo(), "客户", order.getCustomerName());
        row(sheet, 2, "出库日期", text(order.getDeliveryDate()), "状态", statusText(order.getDeliveryStatus()));
        row(sheet, 3, "提货人", order.getPickerName(), "车牌/柜号", join(order.getCarNo(), order.getContainerNo()));
        row(sheet, 4, "签收人", order.getSignUser(), "签收时间", text(order.getSignTime()));
        row(sheet, 5, "总件数", text(order.getTotalCount()), "实物出库总重量kg", text(order.getTotalWeight()));
        row(sheet, 6, "备注", order.getRemark(), "", "");
    }

    private void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(8);
        String[] labels = {
                "序号", "加工单号", "卷号", "实物品名", "实物克重", "实物规格",
                "实物件重kg", "实物出库重量kg", "原纸信息", "加工方式", "工艺摘要", "来源", "成品状态", "备注", "回录备注"
        };
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private void writeItems(Sheet sheet, DeliveryDetailVO detail) {
        int rowIndex = 9;
        int index = 1;
        for (DeliveryDetailItemVO item : detail.getDetails()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(index++);
            row.createCell(1).setCellValue(text(item.getOrderNo()));
            row.createCell(2).setCellValue(finishRollNoText(item));
            row.createCell(3).setCellValue(text(item.getPaperName()));
            row.createCell(4).setCellValue(text(item.getGramWeight()));
            row.createCell(5).setCellValue(specText(item));
            row.createCell(6).setCellValue(text(item.getActualWeight()));
            row.createCell(7).setCellValue(text(item.getOutWeight()));
            row.createCell(8).setCellValue(originalSnapshotText(item));
            row.createCell(9).setCellValue(text(item.getProcessModeText()));
            row.createCell(10).setCellValue(text(item.getProcessSummary()));
            row.createCell(11).setCellValue(sourceText(item.getSourceType()));
            row.createCell(12).setCellValue(finishStatusText(item.getFinishStatus()));
            row.createCell(13).setCellValue(text(item.getRemark()));
            row.createCell(14).setCellValue(text(item.getActualRemark()));
        }
    }

    private void row(Sheet sheet, int rowIndex, String k1, String v1, String k2, String v2) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(k1);
        row.createCell(1).setCellValue(text(v1));
        row.createCell(3).setCellValue(k2);
        row.createCell(4).setCellValue(text(v2));
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

}

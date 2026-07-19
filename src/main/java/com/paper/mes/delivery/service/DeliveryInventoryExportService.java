package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryInventoryExportService {

    private static final int BATCH_SIZE = 500;
    private static final List<String> HEADERS = List.of(
            "客户", "仓库", "仓库位置", "成品卷号", "加工单", "加工单日期", "品名", "规格",
            "首次入库时间", "库龄(天)", "实际重量kg", "剩余重量kg", "计划出库重量kg", "类型", "状态", "待出库单");

    private final DeliveryInventoryMapper inventoryMapper;

    public void exportToPath(DeliveryInventoryFinishQuery query, Path target) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        try (workbook; OutputStream output = Files.newOutputStream(target)) {
            writeWorkbook(workbook, query);
            workbook.write(output);
        } catch (IOException exception) {
            throw new BusinessException("导出成品库存失败");
        } finally {
            workbook.dispose();
        }
    }

    private void writeWorkbook(SXSSFWorkbook workbook, DeliveryInventoryFinishQuery query) {
        Sheet sheet = workbook.createSheet("成品库存");
        writeHeader(sheet);
        writeRows(sheet, query);
    }

    private void writeRows(Sheet sheet, DeliveryInventoryFinishQuery query) {
        long offset = 0;
        int rowIndex = 1;
        while (true) {
            List<DeliveryInventoryFinishVO> rows = inventoryMapper.finishRows(query, offset, BATCH_SIZE);
            for (DeliveryInventoryFinishVO item : rows) writeRow(sheet.createRow(rowIndex++), item);
            if (rows.size() < BATCH_SIZE) return;
            offset += BATCH_SIZE;
        }
    }

    private void writeHeader(Sheet sheet) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < HEADERS.size(); index++) {
            row.createCell(index).setCellValue(HEADERS.get(index));
        }
    }

    private void writeRow(Row row, DeliveryInventoryFinishVO item) {
        List<String> values = List.of(
                text(item.getCustomerName()), text(item.getWarehouseName()), text(item.getWarehouseLocation()),
                text(item.getFinishRollNo()), text(item.getOrderNo()), text(item.getOrderDate()),
                text(item.getPaperName()), specification(item), text(item.getStockInTime()), text(item.getStockAgeDays()),
                text(item.getActualWeight()), text(item.getRemainingWeight()), text(item.getPlannedOutWeight()),
                typeText(item), stockText(item), text(item.getDeliveryNo()));
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    private String specification(DeliveryInventoryFinishVO item) {
        return text(item.getGramWeight()) + "g / " + text(item.getFinishWidth()) + "mm";
    }

    private String typeText(DeliveryInventoryFinishVO item) {
        if (Integer.valueOf(1).equals(item.getIsRemain())) return "余料";
        return Integer.valueOf(2).equals(item.getSourceType()) ? "原纸直发" : "普通成品";
    }

    private String stockText(DeliveryInventoryFinishVO item) {
        return Integer.valueOf(1).equals(item.getStockState()) ? "可出库" : "待出库占用";
    }

    private String text(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}

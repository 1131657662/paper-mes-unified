package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import com.paper.mes.exporttask.service.DeliveryExportSnapshotMetadata;
import com.paper.mes.exporttask.service.DeliveryExportSnapshotReader;
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
    private final DeliveryExportSnapshotReader snapshotReader;

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

    public void exportSnapshotToPath(String snapshotUuid, Path target) {
        DeliveryExportSnapshotMetadata metadata = snapshotReader.metadata(snapshotUuid);
        requireSnapshotType(metadata, DeliveryExportSnapshotMetadata.INVENTORY_TYPE);
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        try (workbook; OutputStream output = Files.newOutputStream(target)) {
            writeSnapshotWorkbook(workbook, metadata);
            workbook.write(output);
        } catch (IOException exception) {
            throw new BusinessException("导出成品库存快照失败");
        } finally {
            workbook.dispose();
        }
    }

    private void writeWorkbook(SXSSFWorkbook workbook, DeliveryInventoryFinishQuery query) {
        Sheet sheet = workbook.createSheet("成品库存");
        writeHeader(sheet);
        writeRows(sheet, query);
    }

    private void writeSnapshotWorkbook(SXSSFWorkbook workbook, DeliveryExportSnapshotMetadata metadata) {
        Sheet sheet = workbook.createSheet("成品库存");
        writeHeader(sheet);
        long rowNo = 0;
        while (true) {
            List<DeliveryInventoryFinishVO> rows = snapshotReader.rows(
                    metadata.snapshotUuid(), rowNo, BATCH_SIZE, DeliveryInventoryFinishVO.class);
            for (DeliveryInventoryFinishVO item : rows) writeRow(sheet.createRow((int) ++rowNo), item);
            if (rows.size() < BATCH_SIZE) break;
        }
        writeSnapshotInfo(workbook.createSheet("导出说明"), metadata);
    }

    private void writeSnapshotInfo(Sheet sheet, DeliveryExportSnapshotMetadata metadata) {
        sheet.createRow(0).createCell(0).setCellValue("快照编号");
        sheet.getRow(0).createCell(1).setCellValue(metadata.snapshotUuid());
        sheet.createRow(1).createCell(0).setCellValue("数据截止时间");
        sheet.getRow(1).createCell(1).setCellValue(metadata.capturedAt().toString().replace('T', ' '));
        sheet.createRow(2).createCell(0).setCellValue("导出行数");
        sheet.getRow(2).createCell(1).setCellValue(metadata.rowCount());
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
        if (Integer.valueOf(2).equals(item.getSourceType())) return "原纸直发";
        if (Integer.valueOf(3).equals(item.getSourceType())) return "整理成品";
        return "普通成品";
    }

    private String stockText(DeliveryInventoryFinishVO item) {
        return Integer.valueOf(1).equals(item.getStockState()) ? "可出库" : "待出库占用";
    }

    private String text(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private void requireSnapshotType(DeliveryExportSnapshotMetadata metadata, String expectedType) {
        if (!expectedType.equals(metadata.snapshotType())) {
            throw new BusinessException("导出数据快照类型不匹配");
        }
    }
}

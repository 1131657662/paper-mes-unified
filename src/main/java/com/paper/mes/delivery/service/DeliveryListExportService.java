package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.delivery.service.impl.DeliveryOrderQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryListExportService {

    private static final int BATCH_SIZE = 100;
    private static final List<String> HEADERS = List.of(
            "出库单号", "客户", "出库日期", "卷数", "出库重量kg", "提货人", "车牌",
            "柜号", "状态", "结算拦截", "签收人", "签收时间", "备注", "创建时间");

    private final DeliveryOrderMapper deliveryOrderMapper;

    public void exportToPath(DeliveryQuery query, Path target) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(BATCH_SIZE);
        try (workbook; OutputStream output = Files.newOutputStream(target)) {
            writeWorkbook(workbook, query);
            workbook.write(output);
        } catch (IOException exception) {
            throw new BusinessException("导出出库对账失败");
        } finally {
            workbook.dispose();
        }
    }

    private void writeWorkbook(SXSSFWorkbook workbook, DeliveryQuery query) {
        Sheet sheet = workbook.createSheet("出库对账");
        writeHeader(sheet);
        writeRows(sheet, query);
    }

    private void writeRows(Sheet sheet, DeliveryQuery query) {
        long current = 1;
        int rowIndex = 1;
        while (true) {
            Page<DeliveryOrder> page = deliveryOrderMapper.selectPage(
                    Page.of(current, BATCH_SIZE, false), DeliveryOrderQueryBuilder.build(query));
            for (DeliveryOrder order : page.getRecords()) {
                writeRow(sheet.createRow(rowIndex++), order);
            }
            if (page.getRecords().size() < BATCH_SIZE) return;
            current++;
        }
    }

    private void writeHeader(Sheet sheet) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < HEADERS.size(); index++) {
            row.createCell(index).setCellValue(HEADERS.get(index));
        }
    }

    private void writeRow(Row row, DeliveryOrder order) {
        List<String> values = List.of(
                text(order.getDeliveryNo()), text(order.getCustomerName()), date(order.getDeliveryDate()),
                text(order.getTotalCount()), text(order.getTotalWeight()), text(order.getPickerName()),
                text(order.getCarNo()), text(order.getContainerNo()), statusText(order.getDeliveryStatus()),
                blockText(order.getSettleBlockAction()), text(order.getSignUser()), dateTime(order.getSignTime()),
                text(order.getRemark()), dateTime(order.getCreateTime()));
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    private String statusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 1 -> "待出库";
            case 2 -> "已出库";
            case 3 -> "已作废";
            default -> String.valueOf(status);
        };
    }

    private String blockText(Integer action) {
        if (action == null || action == 0) return "-";
        return action == 1 ? "警告放行" : "强制拦截";
    }

    private String date(LocalDate value) {
        return value == null ? "-" : value.toString();
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }

    private String text(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}

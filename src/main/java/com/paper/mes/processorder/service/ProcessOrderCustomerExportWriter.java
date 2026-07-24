package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

import static com.paper.mes.processorder.service.ProcessOrderExportText.value;

final class ProcessOrderCustomerExportWriter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ProcessOrderCustomerExportWriter() {
    }

    static void write(Sheet sheet, ProcessOrderDetailVO detail, CellStyle headerStyle) {
        writeHeader(sheet, headerStyle);
        Map<String, BigDecimal> fallbackWeights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(detail.getRollProductions());
        int rowIndex = 1;
        for (FinishRoll finish : detail.getFinishRolls()) {
            if (!isCustomerProduct(finish)) continue;
            Object[] values = customerRow(finish, fallbackWeights);
            writeRow(sheet.createRow(rowIndex), rowIndex++, values);
        }
        autoSize(sheet, 15);
    }

    private static Object[] customerRow(FinishRoll finish, Map<String, BigDecimal> fallbackWeights) {
        BigDecimal physicalWeight = finish.getActualWeight() != null
                ? finish.getActualWeight()
                : ProcessOrderExportWeightResolver.estimateWeight(finish, fallbackWeights);
        String customerPaper = fallback(finish.getCustomerPaperName(), finish.getPaperName());
        Integer customerGsm = fallback(finish.getCustomerGramWeight(), finish.getGramWeight());
        Integer customerWidth = fallback(finish.getCustomerFinishWidth(), finish.getFinishWidth());
        BigDecimal customerWeight = fallback(finish.getCustomerDisplayWeight(), physicalWeight);
        boolean changed = !Objects.equals(finish.getPaperName(), customerPaper)
                || !Objects.equals(finish.getGramWeight(), customerGsm)
                || !Objects.equals(finish.getFinishWidth(), customerWidth)
                || different(customerWeight, physicalWeight);
        String changedAt = finish.getCustomerSpecOverrideAt() == null
                ? null : TIME_FORMAT.format(finish.getCustomerSpecOverrideAt());
        return new Object[]{finish.getFinishRollNo(), finish.getPaperName(), finish.getGramWeight(),
                finish.getFinishWidth(), physicalWeight, customerPaper, customerGsm, customerWidth,
                customerWeight, changed ? "已调整" : "与实物一致", finish.getCustomerSpecOverrideReason(),
                finish.getCustomerSpecOverrideBy(), changedAt, finish.getRemark()};
    }

    private static boolean different(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) return !Objects.equals(left, right);
        return left.compareTo(right) != 0;
    }

    private static boolean isCustomerProduct(FinishRoll finish) {
        return !Integer.valueOf(1).equals(finish.getIsRemain())
                && !Integer.valueOf(1).equals(finish.getIsSpare())
                && !Integer.valueOf(3).equals(finish.getRollNoStatus());
    }

    private static <T> T fallback(T preferred, T physical) {
        return preferred == null ? physical : preferred;
    }

    private static String fallback(String preferred, String physical) {
        return preferred == null || preferred.isBlank() ? physical : preferred.trim();
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        String[] labels = {"序号", "成品卷号", "实物品名", "实物克重", "实物门幅", "实物重量kg",
                "客户品名", "客户克重", "客户门幅", "客户重量kg", "口径状态", "调整原因",
                "调整人", "调整时间", "成品备注"};
        Row row = sheet.createRow(0);
        for (int index = 0; index < labels.length; index++) {
            row.createCell(index).setCellValue(labels[index]);
            row.getCell(index).setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void writeRow(Row row, int rowNumber, Object[] values) {
        row.createCell(0).setCellValue(value(rowNumber));
        for (int index = 0; index < values.length; index++) {
            row.createCell(index + 1).setCellValue(value(values[index]));
        }
    }

    private static void autoSize(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12000));
        }
    }
}

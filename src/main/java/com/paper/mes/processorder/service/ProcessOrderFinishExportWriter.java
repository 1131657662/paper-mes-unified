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

import static com.paper.mes.processorder.service.ProcessOrderExportText.*;

final class ProcessOrderFinishExportWriter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int COLUMN_COUNT = 25;

    private ProcessOrderFinishExportWriter() {
    }

    static void write(Sheet sheet, ProcessOrderDetailVO detail, CellStyle headerStyle) {
        writeHeader(sheet, headerStyle);
        Map<String, BigDecimal> fallbackWeights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(detail.getRollProductions());
        int rowIndex = 1;
        for (FinishRoll finish : detail.getFinishRolls()) {
            writeFinish(sheet.createRow(rowIndex++), finish, fallbackWeights);
        }
        autoSize(sheet);
    }

    private static void writeFinish(Row row, FinishRoll finish, Map<String, BigDecimal> fallbackWeights) {
        BigDecimal physicalWeight = finish.getActualWeight() != null
                ? finish.getActualWeight()
                : ProcessOrderExportWeightResolver.estimateWeight(finish, fallbackWeights);
        Object[] values = {row.getRowNum(), finish.getFinishRollNo(), finish.getFinishInnerNo(), finish.getPaperName(),
                finish.getGramWeight(), finish.getFinishWidth(), finish.getFinishDiameter(),
                finish.getFinishCoreDiameter(), sourceText(finish.getSourceType()),
                ProcessOrderExportWeightResolver.estimateWeight(finish, fallbackWeights), finish.getActualWeight(),
                finish.getTrimWeightShare(), finishStatusText(finish.getFinishStatus()), yesNoText(finish.getIsSpare()),
                finish.getOriginalRollNos(), finish.getActualRemark(), finish.getRemark()};
        writeValues(row, values);
        writeCustomerValues(row, finish, physicalWeight);
    }

    private static void writeCustomerValues(Row row, FinishRoll finish, BigDecimal physicalWeight) {
        if (!isCustomerProduct(finish)) {
            writeValuesFrom(row, 17, new Object[]{null, null, null, null, null, null, null, null});
            return;
        }
        String customerPaper = fallback(finish.getCustomerPaperName(), finish.getPaperName());
        Integer customerGsm = fallback(finish.getCustomerGramWeight(), finish.getGramWeight());
        Integer customerWidth = fallback(finish.getCustomerFinishWidth(), finish.getFinishWidth());
        BigDecimal customerWeight = fallback(finish.getCustomerDisplayWeight(), physicalWeight);
        CustomerValues customer = new CustomerValues(customerPaper, customerGsm, customerWidth, customerWeight);
        String status = isChanged(finish, customer, physicalWeight) ? "已调整" : "与实物一致";
        String changedAt = finish.getCustomerSpecOverrideAt() == null
                ? null : TIME_FORMAT.format(finish.getCustomerSpecOverrideAt());
        writeValuesFrom(row, 17, new Object[]{customer.paper(), customer.gsm(), customer.width(), customer.weight(), status,
                finish.getCustomerSpecOverrideReason(), finish.getCustomerSpecOverrideBy(), changedAt});
    }

    private static boolean isChanged(FinishRoll finish, CustomerValues customer, BigDecimal physicalWeight) {
        return !Objects.equals(finish.getPaperName(), customer.paper())
                || !Objects.equals(finish.getGramWeight(), customer.gsm())
                || !Objects.equals(finish.getFinishWidth(), customer.width())
                || different(customer.weight(), physicalWeight);
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
        String[] labels = {"序号", "成品卷号", "内部号", "品名", "克重", "门幅", "外径", "纸芯", "来源",
                "预估重量kg", "实际重量kg", "切边kg", "状态", "备用号", "来源原卷", "回录备注", "备注",
                "客户品名", "客户克重", "客户门幅", "客户重量kg", "客户口径状态", "调整原因", "调整人", "调整时间"};
        Row row = sheet.createRow(0);
        writeValues(row, labels);
        for (int index = 0; index < labels.length; index++) row.getCell(index).setCellStyle(style);
        sheet.createFreezePane(0, 1);
    }

    private static void writeValues(Row row, Object[] values) {
        writeValuesFrom(row, 0, values);
    }

    private static void writeValuesFrom(Row row, int start, Object[] values) {
        for (int index = 0; index < values.length; index++) {
            row.createCell(start + index).setCellValue(value(values[index]));
        }
    }

    private static void autoSize(Sheet sheet) {
        for (int index = 0; index < COLUMN_COUNT; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12000));
        }
    }

    private record CustomerValues(String paper, Integer gsm, Integer width, BigDecimal weight) {
    }
}

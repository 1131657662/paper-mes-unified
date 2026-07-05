package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

import static com.paper.mes.processorder.service.ProcessOrderExportText.productionLabel;
import static com.paper.mes.processorder.service.ProcessOrderExportText.stepTypeText;
import static com.paper.mes.processorder.service.ProcessOrderExportText.value;

final class ProcessOrderStageOutputExportWriter {

    private ProcessOrderStageOutputExportWriter() {
    }

    static void write(Sheet sheet, List<ProcessOrderDetailVO.RollProductionVO> productions) {
        header(sheet, "序号", "所属原卷", "产物编号", "父级产物", "阶段", "产物类型", "状态",
                "品名", "克重", "门幅", "外径", "纸芯", "预估重量kg", "实际重量kg", "来源工艺");
        int rowIndex = 1;
        for (ProcessOrderDetailVO.RollProductionVO production : productions) {
            for (ProcessOrderDetailVO.StageOutputVO output : stageOutputs(production)) {
                Row row = sheet.createRow(rowIndex++);
                cells(row, rowIndex - 1, productionLabel(production), output.getOutputNo(),
                        output.getParentOutputUuid(), stageText(output.getStageLevel()),
                        outputTypeText(output.getOutputType()), outputStatusText(output.getOutputStatus()),
                        output.getPaperName(), output.getGramWeight(), output.getFinishWidth(),
                        output.getFinishDiameter(), output.getFinishCoreDiameter(), output.getEstimateWeight(),
                        output.getActualWeight(), sourceText(output));
            }
        }
        autosize(sheet, 15);
    }

    private static void header(Sheet sheet, String... labels) {
        Row row = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void cells(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(value(values[i]));
        }
    }

    private static String sourceText(ProcessOrderDetailVO.StageOutputVO output) {
        return output.getSourceSummary() == null || output.getSourceSummary().isBlank()
                ? stepTypeText(output.getSourceStepType())
                : output.getSourceSummary();
    }

    private static String stageText(Integer value) {
        return value == null ? "-" : "第" + value + "段";
    }

    private static String outputTypeText(Integer value) {
        return value != null && value == 1 ? "中间产物" : "最终产物";
    }

    private static String outputStatusText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 2 -> "进入下道"; case 3 -> "已生成成品"; case 4 -> "已失效"; default -> "计划产物"; };
    }

    private static List<ProcessOrderDetailVO.StageOutputVO> stageOutputs(ProcessOrderDetailVO.RollProductionVO production) {
        return production.getStageOutputs() == null ? List.of() : production.getStageOutputs();
    }

    private static void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }
}

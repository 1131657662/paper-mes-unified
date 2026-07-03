package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.paper.mes.processorder.service.ProcessOrderExportText.*;

/**
 * 加工单详情资料 Excel 导出装配。
 */
@Service
public class ProcessOrderExportService {

    public Workbook buildWorkbook(ProcessOrderDetailVO detail) {
        Workbook workbook = new XSSFWorkbook();
        Styles styles = styles(workbook);
        writeSummary(workbook.createSheet("基础信息"), detail.getOrder(), styles);
        writeOriginals(workbook.createSheet("原卷信息"), detail.getOriginalRolls(), styles);
        writeSteps(workbook.createSheet("工序费用"), detail.getSteps(), detail.getOriginalRolls(), styles);
        writeRewindParams(workbook.createSheet("复卷参数"), detail.getRollProductions(), styles);
        writeFinishes(workbook.createSheet("成品明细"), detail.getFinishRolls(), detail.getRollProductions(), styles);
        writeSources(workbook.createSheet("成品来源"), detail.getRollProductions(), styles);
        return workbook;
    }

    private void writeSummary(Sheet sheet, ProcessOrder order, Styles styles) {
        title(sheet, styles, "加工单资料");
        row(sheet, 1, "加工单号", order.getOrderNo(), "客户", order.getCustomerName());
        row(sheet, 2, "制单日期", order.getOrderDate(), "期望完成", order.getExpectFinishDate());
        row(sheet, 3, "状态", statusText(order.getOrderStatus()), "优先级", priorityText(order.getPriority()));
        row(sheet, 4, "结算方式", settleText(order.getSettleType()), "是否开票", invoiceText(order.getIsInvoice()));
        row(sheet, 5, "原卷重量kg", order.getTotalOriginalWeight(), "成品重量kg", order.getTotalFinishWeight());
        row(sheet, 6, "加工费", order.getTotalProcessAmount(), "附加费", order.getTotalExtraAmount());
        row(sheet, 7, "应收合计", order.getTotalAmount(), "税点", order.getTaxRate());
        row(sheet, 8, "打印次数", order.getPrintCount(), "最后打印", order.getLastPrintTime());
        row(sheet, 9, "备注", join(order.getRemark(), order.getRemarkLong()), "", "");
        autosize(sheet, 5);
    }

    private void writeOriginals(Sheet sheet, List<OriginalRoll> rolls, Styles styles) {
        header(sheet, styles, "序号", "编号", "卷号", "批号", "品名", "克重", "实测克重", "门幅",
                "实测门幅", "直径", "纸芯", "长度m", "件重kg", "实重kg", "件数", "总重kg",
                "加工模式", "主工艺", "机台", "操作人", "加工费", "损耗kg", "损耗率", "状态", "备注");
        int rowIndex = 1;
        for (OriginalRoll roll : rolls) {
            Row row = sheet.createRow(rowIndex++);
            cells(row, rowIndex - 1, roll.getExtraNo(), roll.getRollNo(), roll.getBatchNo(), roll.getPaperName(),
                    roll.getGramWeight(), roll.getActualGramWeight(), roll.getOriginalWidth(), roll.getActualWidth(),
                    roll.getOriginalDiameter(), roll.getCoreDiameter(), roll.getOriginalLength(), roll.getRollWeight(),
                    roll.getActualWeight(), roll.getPieceNum(), roll.getTotalWeight(), processModeText(roll.getProcessMode()),
                    stepTypeText(roll.getMainStepType()), roll.getMachineUuid(), roll.getOperator(),
                    roll.getProcessAmount(), roll.getTotalLossWeight(), roll.getTotalLossRatio(),
                    rollStatusText(roll.getRollStatus()), join(roll.getRemark(), roll.getDamageDesc()));
        }
        autosize(sheet, 25);
    }

    private void writeSteps(Sheet sheet, List<ProcessStep> steps, List<OriginalRoll> rolls, Styles styles) {
        header(sheet, styles, "序号", "所属原卷", "阶段", "输入", "工序", "主工艺", "刀数",
                "加工重量kg", "单价", "金额", "损耗kg", "操作人", "备注");
        int rowIndex = 1;
        for (ProcessStep step : steps) {
            Row row = sheet.createRow(rowIndex++);
            cells(row, rowIndex - 1, rollLabel(rolls, step.getOriginalUuid()), stageText(step),
                    inputText(step), stepTypeText(step.getStepType()), yesNoText(step.getIsMain()),
                    step.getKnifeCount(), step.getProcessWeight(), step.getUnitPrice(), step.getStepAmount(),
                    step.getLossWeight(), step.getOperator(), step.getRemark());
        }
        autosize(sheet, 13);
    }

    private void writeRewindParams(Sheet sheet, List<ProcessOrderDetailVO.RollProductionVO> rows, Styles styles) {
        header(sheet, styles, "序号", "原卷", "层号", "成品门幅", "外径", "纸芯", "面积占比",
                "分切占比", "模式", "备注");
        int rowIndex = 1;
        for (ProcessOrderDetailVO.RollProductionVO rowData : rows) {
            for (ProcessOrderDetailVO.RewindParamVO param : rowData.getRewindParams()) {
                Row row = sheet.createRow(rowIndex++);
                cells(row, rowIndex - 1, productionLabel(rowData), param.getLayerSort(), param.getLayerWidth(),
                        param.getOutDiameter(), param.getCoreDiameter(), param.getAreaRatio(),
                        param.getSplitRatio(), paramModeText(param.getParamMode()), param.getRemark());
            }
        }
        autosize(sheet, 10);
    }

    private void writeFinishes(Sheet sheet, List<FinishRoll> finishes,
                               List<ProcessOrderDetailVO.RollProductionVO> productions,
                               Styles styles) {
        header(sheet, styles, "序号", "成品卷号", "内部号", "品名", "克重", "门幅", "外径",
                "纸芯", "来源", "预估重量kg", "实际重量kg", "切边kg", "状态", "备用号",
                "来源原卷", "回录备注", "备注");
        Map<String, BigDecimal> fallbackWeights = ProcessOrderExportWeightResolver.fallbackEstimateWeights(productions);
        int rowIndex = 1;
        for (FinishRoll finish : finishes) {
            Row row = sheet.createRow(rowIndex++);
            cells(row, rowIndex - 1, finish.getFinishRollNo(), finish.getFinishInnerNo(), finish.getPaperName(),
                    finish.getGramWeight(), finish.getFinishWidth(), finish.getFinishDiameter(),
                    finish.getFinishCoreDiameter(), sourceText(finish.getSourceType()),
                    ProcessOrderExportWeightResolver.estimateWeight(finish, fallbackWeights),
                    finish.getActualWeight(), finish.getTrimWeightShare(), finishStatusText(finish.getFinishStatus()),
                    yesNoText(finish.getIsSpare()), finish.getOriginalRollNos(), finish.getActualRemark(),
                    finish.getRemark());
        }
        autosize(sheet, 17);
    }

    private void writeSources(Sheet sheet, List<ProcessOrderDetailVO.RollProductionVO> rows, Styles styles) {
        header(sheet, styles, "序号", "成品卷号", "成品规格", "所属原卷", "来源卷号", "来源品名",
                "分摊比例", "分摊重量kg", "备注");
        int rowIndex = 1;
        for (ProcessOrderDetailVO.RollProductionVO production : rows) {
            rowIndex = writeProductionSources(sheet, production, rowIndex);
        }
        autosize(sheet, 9);
    }

    private int writeProductionSources(Sheet sheet, ProcessOrderDetailVO.RollProductionVO production, int rowIndex) {
        for (ProcessOrderDetailVO.FinishProductionVO finish : production.getFinishes()) {
            if (finish.getSources().isEmpty()) {
                cells(sheet.createRow(rowIndex++), rowIndex - 1, finish.getFinishRollNo(), finishSpec(finish),
                        productionLabel(production), production.getRollNo(), production.getPaperName(), "", "", "");
                continue;
            }
            for (ProcessOrderDetailVO.FinishSourceVO source : finish.getSources()) {
                cells(sheet.createRow(rowIndex++), rowIndex - 1, finish.getFinishRollNo(), finishSpec(finish),
                        productionLabel(production), source.getRollNo(), source.getPaperName(),
                        source.getShareRatio(), source.getShareWeight(), source.getRemark());
            }
        }
        return rowIndex;
    }

    private void title(Sheet sheet, Styles styles, String text) {
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(text);
        row.getCell(0).setCellStyle(styles.title);
    }

    private void header(Sheet sheet, Styles styles, String... labels) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(styles.header);
        }
        sheet.createFreezePane(0, 1);
    }

    private void row(Sheet sheet, int rowIndex, String k1, Object v1, String k2, Object v2) {
        Row row = sheet.createRow(rowIndex);
        cells(row, k1, value(v1), k2, value(v2));
    }

    private void cells(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(value(values[i]));
        }
    }

    private Styles styles(Workbook workbook) {
        CellStyle title = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        title.setFont(titleFont);
        CellStyle header = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        header.setFont(headerFont);
        return new Styles(title, header);
    }

    private void autosize(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
        }
    }

    private record Styles(CellStyle title, CellStyle header) {
    }
}

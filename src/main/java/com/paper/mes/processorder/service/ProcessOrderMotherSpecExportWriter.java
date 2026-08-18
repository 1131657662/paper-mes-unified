package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.paper.mes.processorder.service.ProcessOrderExportText.*;

/**
 * 母卷一行、成品加工规格按组展开的导出表。
 */
final class ProcessOrderMotherSpecExportWriter {

    private static final int MOTHER_COLUMN_COUNT = 26;
    private static final int FINISH_COLUMN_COUNT = 3;

    private ProcessOrderMotherSpecExportWriter() {
    }

    static void write(Sheet sheet, ProcessOrderDetailVO detail, CellStyle headerStyle) {
        List<OriginalRoll> rolls = detail.getOriginalRolls() == null
                ? List.of() : detail.getOriginalRolls();
        List<ProcessOrderDetailVO.RollProductionVO> productions = detail.getRollProductions() == null
                ? List.of() : detail.getRollProductions();
        Map<String, ProcessOrderDetailVO.RollProductionVO> productionByRoll = indexProductions(productions);
        int maxFinishCount = maxReportableFinishes(productions);
        writeHeader(sheet, headerStyle, maxFinishCount);
        writeRows(sheet, rolls, productionByRoll, maxFinishCount, productions);
        autoSize(sheet, MOTHER_COLUMN_COUNT + maxFinishCount * FINISH_COLUMN_COUNT);
        sheet.createFreezePane(MOTHER_COLUMN_COUNT, 1);
    }

    private static Map<String, ProcessOrderDetailVO.RollProductionVO> indexProductions(
            List<ProcessOrderDetailVO.RollProductionVO> productions) {
        Map<String, ProcessOrderDetailVO.RollProductionVO> result = new LinkedHashMap<>();
        for (ProcessOrderDetailVO.RollProductionVO production : productions) {
            if (production.getOriginalUuid() != null) {
                result.put(production.getOriginalUuid(), production);
            }
        }
        return result;
    }

    private static int maxReportableFinishes(List<ProcessOrderDetailVO.RollProductionVO> productions) {
        int max = 0;
        for (ProcessOrderDetailVO.RollProductionVO production : productions) {
            max = Math.max(max, reportableFinishes(production).size());
        }
        return max;
    }

    private static void writeHeader(Sheet sheet, CellStyle headerStyle, int maxFinishCount) {
        String[] labels = {"序号", "编号", "卷号", "批号", "品名", "克重", "实测克重", "门幅",
                "实测门幅", "直径", "纸芯", "长度m", "件重kg", "实重kg", "件数", "总重kg",
                "加工模式", "主工艺", "机台", "操作人", "加工费", "损耗kg", "损耗率", "状态",
                "处置动作", "备注"};
        Row row = sheet.createRow(0);
        for (int index = 0; index < labels.length; index++) {
            writeCell(row, index, labels[index], headerStyle);
        }
        for (int index = 0; index < maxFinishCount; index++) {
            int column = MOTHER_COLUMN_COUNT + index * FINISH_COLUMN_COUNT;
            writeCell(row, column, "成品规格" + (index + 1), headerStyle);
            writeCell(row, column + 1, "成品重量" + (index + 1), headerStyle);
            writeCell(row, column + 2, "成品卷号" + (index + 1), headerStyle);
        }
    }

    private static void writeRows(Sheet sheet, List<OriginalRoll> rolls,
                                  Map<String, ProcessOrderDetailVO.RollProductionVO> productionByRoll,
                                  int maxFinishCount,
                                  List<ProcessOrderDetailVO.RollProductionVO> productions) {
        Map<String, ?> fallbackWeights = ProcessOrderExportWeightResolver.fallbackEstimateWeights(productions);
        for (int index = 0; index < rolls.size(); index++) {
            OriginalRoll roll = rolls.get(index);
            ProcessOrderDetailVO.RollProductionVO production = productionByRoll.get(roll.getUuid());
            writeRow(sheet.createRow(index + 1), index + 1, roll, production, maxFinishCount, fallbackWeights);
        }
    }

    private static void writeRow(Row row, int rowNumber, OriginalRoll roll,
                                 ProcessOrderDetailVO.RollProductionVO production,
                                 int maxFinishCount, Map<String, ?> fallbackWeights) {
        writeValues(row, rowNumber, roll.getExtraNo(), roll.getRollNo(), roll.getBatchNo(), roll.getPaperName(),
                roll.getGramWeight(), roll.getActualGramWeight(), roll.getOriginalWidth(), roll.getActualWidth(),
                roll.getOriginalDiameter(), roll.getCoreDiameter(), roll.getOriginalLength(), roll.getRollWeight(),
                roll.getActualWeight(), roll.getPieceNum(), roll.getTotalWeight(), processModeText(roll.getProcessMode()),
                stepTypeText(roll.getMainStepType()), roll.getMachineUuid(), roll.getOperator(), roll.getProcessAmount(),
                roll.getTotalLossWeight(), roll.getTotalLossRatio(), rollStatusText(roll.getRollStatus()),
                dispositionText(roll.getDispositionAction()), join(roll.getRemark(), roll.getDamageDesc()));
        writeFinishes(row, roll.getUuid(), reportableFinishes(production), maxFinishCount, fallbackWeights);
    }

    private static void writeFinishes(Row row, String originalUuid,
                                      List<ProcessOrderDetailVO.FinishProductionVO> finishes,
                                      int maxFinishCount, Map<String, ?> fallbackWeights) {
        for (int index = 0; index < maxFinishCount; index++) {
            int column = MOTHER_COLUMN_COUNT + index * FINISH_COLUMN_COUNT;
            if (index >= finishes.size()) {
                writeCell(row, column, "", null);
                writeCell(row, column + 1, "", null);
                writeCell(row, column + 2, "", null);
                continue;
            }
            ProcessOrderDetailVO.FinishProductionVO finish = finishes.get(index);
            writeCell(row, column, value(finish.getFinishWidth()), null);
            writeCell(row, column + 1, value(finishWeight(finish, originalUuid, fallbackWeights)), null);
            writeCell(row, column + 2, value(finish.getFinishRollNo()), null);
        }
    }

    private static List<ProcessOrderDetailVO.FinishProductionVO> reportableFinishes(
            ProcessOrderDetailVO.RollProductionVO production) {
        if (production == null || production.getFinishes() == null) {
            return List.of();
        }
        List<ProcessOrderDetailVO.FinishProductionVO> result = new ArrayList<>();
        for (ProcessOrderDetailVO.FinishProductionVO finish : production.getFinishes()) {
            if (isReportableFinish(finish)) {
                result.add(finish);
            }
        }
        return result;
    }

    private static boolean isReportableFinish(ProcessOrderDetailVO.FinishProductionVO finish) {
        return finish != null
                && !Integer.valueOf(1).equals(finish.getIsRemain())
                && !Integer.valueOf(1).equals(finish.getIsSpare())
                && !Integer.valueOf(3).equals(finish.getRollNoStatus());
    }

    private static Object finishWeight(ProcessOrderDetailVO.FinishProductionVO finish, String originalUuid,
                                       Map<String, ?> fallbackWeights) {
        List<ProcessOrderDetailVO.FinishSourceVO> sources = finish.getSources() == null
                ? List.of() : finish.getSources();
        for (ProcessOrderDetailVO.FinishSourceVO source : sources) {
            if (Objects.equals(originalUuid, source.getOriginalUuid())) {
                if (source.getShareWeight() != null) {
                    return source.getShareWeight();
                }
                return sources.size() == 1 ? fallbackWeight(finish, fallbackWeights) : null;
            }
        }
        return sources.isEmpty() ? fallbackWeight(finish, fallbackWeights) : null;
    }

    private static Object fallbackWeight(ProcessOrderDetailVO.FinishProductionVO finish,
                                         Map<String, ?> fallbackWeights) {
        if (finish.getActualWeight() != null) {
            return finish.getActualWeight();
        }
        if (finish.getEstimateWeight() != null) {
            return finish.getEstimateWeight();
        }
        Object byUuid = fallbackWeights.get(finish.getUuid());
        return byUuid == null ? fallbackWeights.get(finish.getFinishRollNo()) : byUuid;
    }

    private static void writeValues(Row row, Object... values) {
        for (int index = 0; index < values.length; index++) {
            writeCell(row, index, value(values[index]), null);
        }
    }

    private static void writeCell(Row row, int column, String text, CellStyle style) {
        row.createCell(column).setCellValue(text);
        if (style != null) {
            row.getCell(column).setCellStyle(style);
        }
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12000));
        }
    }
}

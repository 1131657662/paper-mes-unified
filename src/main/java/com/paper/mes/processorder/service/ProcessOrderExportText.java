package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;

import java.math.BigDecimal;
import java.util.List;

final class ProcessOrderExportText {

    private ProcessOrderExportText() {
    }

    static String rollLabel(List<OriginalRoll> rolls, String uuid) {
        return rolls.stream().filter(roll -> uuid != null && uuid.equals(roll.getUuid())).findFirst()
                .map(roll -> productionLabel(roll.getRowSort(), roll.getRollNo(), roll.getExtraNo()))
                .orElse("-");
    }

    static String productionLabel(ProcessOrderDetailVO.RollProductionVO row) {
        return productionLabel(null, row.getRollNo(), row.getExtraNo());
    }

    static String finishSpec(ProcessOrderDetailVO.FinishProductionVO finish) {
        return value(finish.getPaperName()) + " / " + value(finish.getGramWeight()) + "g / "
                + value(finish.getFinishWidth()) + "mm";
    }

    static BigDecimal estimateWeight(FinishRoll finish) {
        return finish.getEstimateWeight() == null ? finish.getEstimateWeightSnap() : finish.getEstimateWeight();
    }

    static String stageText(ProcessStep step) {
        return step.getStageLevel() == null ? "-" : "第" + step.getStageLevel() + "段";
    }

    static String inputText(ProcessStep step) {
        if (step.getInputType() == null) return "-";
        return step.getInputType() == 2 ? "上一阶段产出" : "原卷";
    }

    static String value(Object value) {
        if (value == null) return "-";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return value.toString();
    }

    static String join(String first, String second) {
        if (first == null || first.isBlank()) return value(second);
        if (second == null || second.isBlank()) return first;
        return first + "；" + second;
    }

    static String priorityText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 2 -> "加急"; case 3 -> "特急"; default -> "普通"; };
    }

    static String statusText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 0 -> "草稿"; case 1 -> "待下发"; case 2 -> "加工中"; case 3 -> "待回录"; case 4 -> "已完成"; case 5 -> "已结算"; case 6 -> "已作废"; default -> value.toString(); };
    }

    static String processModeText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 2 -> "现场定尺"; case 3 -> "直发"; default -> "标准加工"; };
    }

    static String stepTypeText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 1 -> "锯纸"; case 2 -> "复卷"; default -> value.toString(); };
    }

    static String rollStatusText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 1 -> "待加工"; case 2 -> "加工中"; case 3 -> "完成"; case 4 -> "直发"; case 5 -> "报废"; default -> value.toString(); };
    }

    static String finishStatusText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 1 -> "待入库"; case 2 -> "已入库"; case 3 -> "已出库"; case 4 -> "报废"; default -> value.toString(); };
    }

    static String sourceText(Integer value) {
        if (value == null) return "-";
        return value == 2 ? "原纸直发" : "加工产出";
    }

    static String settleText(Integer value) {
        if (value == null) return "-";
        return value == 1 ? "次结" : value == 2 ? "月结" : value.toString();
    }

    static String invoiceText(Integer value) {
        if (value == null) return "-";
        return value == 1 ? "开票" : "不开票";
    }

    static String yesNoText(Integer value) {
        return value != null && value == 1 ? "是" : "否";
    }

    static String paramModeText(Integer value) {
        if (value == null) return "-";
        return value == 2 ? "按比例" : "按门幅";
    }

    private static String productionLabel(Integer sort, String rollNo, String extraNo) {
        String prefix = sort == null ? "原卷" : "原卷" + sort;
        String label = rollNo != null && !rollNo.isBlank() ? rollNo : extraNo;
        return prefix + " / " + value(label);
    }
}

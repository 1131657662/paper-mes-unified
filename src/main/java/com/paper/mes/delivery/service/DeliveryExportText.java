package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryDetailItemVO;

import java.math.BigDecimal;

final class DeliveryExportText {

    private DeliveryExportText() {
    }

    static String specText(DeliveryDetailItemVO item) {
        StringBuilder value = new StringBuilder();
        append(value, item.getFinishWidth() == null ? null : item.getFinishWidth() + "mm");
        append(value, item.getFinishDiameter() == null ? null : "φ" + item.getFinishDiameter());
        append(value, item.getFinishCoreDiameter() == null ? null : "芯" + item.getFinishCoreDiameter());
        return value.isEmpty() ? "-" : value.toString();
    }

    static String join(String left, String right) {
        if ((left == null || left.isBlank()) && (right == null || right.isBlank())) return "-";
        return text(left) + " / " + text(right);
    }

    static String sourceText(Integer value) {
        if (value == null) return "-";
        return switch (value) { case 2 -> "直发原纸"; case 3 -> "整理成品"; default -> "加工产出"; };
    }

    static String statusText(Integer value) {
        if (value == null) return "-";
        return value == 2 ? "已出库签收" : "待出库";
    }

    static String finishStatusText(Integer value) {
        if (value == null) return "-";
        return switch (value) {
            case 1 -> "待入库";
            case 2 -> "已入库";
            case 3 -> "已出库";
            case 4 -> "报废";
            default -> String.valueOf(value);
        };
    }

    static String finishRollNoText(DeliveryDetailItemVO item) {
        String rollNo = text(item.getFinishRollNo());
        return Integer.valueOf(1).equals(item.getIsRemain()) ? rollNo + "（余料）" : rollNo;
    }

    static String originalSnapshotText(DeliveryDetailItemVO item) {
        if (item.getOriginalItems() == null || item.getOriginalItems().isEmpty()) {
            return firstText(item.getOriginalSummary(), item.getOriginalRollNos());
        }
        return item.getOriginalItems().stream().map(DeliveryExportText::originalSourceText)
                .reduce((left, right) -> left + "；" + right).orElse("-");
    }

    static String text(Object value) {
        if (value == null) return "-";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return value.toString();
    }

    private static String originalSourceText(DeliveryDetailItemVO.OriginalSourceItem item) {
        StringBuilder value = new StringBuilder();
        append(value, item.getRowSort() == null ? "母卷" : "母卷" + item.getRowSort());
        append(value, item.getRollNo() == null ? null : "卷号" + item.getRollNo());
        append(value, item.getExtraNo() == null ? null : "编号" + item.getExtraNo());
        append(value, item.getPaperName());
        append(value, item.getGramWeight() == null ? null : item.getGramWeight() + "g");
        append(value, item.getOriginalWidth() == null ? null : item.getOriginalWidth() + "mm");
        BigDecimal weight = item.getActualWeight() == null ? item.getTotalWeight() : item.getActualWeight();
        append(value, weight == null ? null : text(weight) + "kg");
        append(value, item.getMachineName() == null ? null : "机台" + item.getMachineName());
        return value.isEmpty() ? "-" : value.toString();
    }

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : text(second);
    }

    private static void append(StringBuilder target, String value) {
        if (value == null || value.isBlank()) return;
        if (!target.isEmpty()) target.append(" / ");
        target.append(value);
    }
}

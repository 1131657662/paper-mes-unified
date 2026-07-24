package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class SettleBillLineText {

    private SettleBillLineText() {
    }

    static String originalText(SettlePrintLineVO line) {
        String label = text(line.getOriginalLabel());
        String rollNo = line.getOriginalRollNo();
        String extraNo = line.getOriginalExtraNo();
        StringBuilder value = new StringBuilder(label);
        append(value, rollNo == null || rollNo.equals(line.getOriginalLabel()) ? null : "卷号 " + rollNo);
        append(value, extraNo == null ? null : "编号 " + extraNo);
        return value.toString();
    }

    static String originalSpec(SettlePrintLineVO line) {
        Integer gram = line.getActualGramWeight() == null ? line.getGramWeight() : line.getActualGramWeight();
        Integer width = line.getActualWidth() == null ? line.getOriginalWidth() : line.getActualWidth();
        StringBuilder value = new StringBuilder(text(line.getPaperName()));
        append(value, gram == null ? null : gram + " g");
        append(value, width == null ? null : width + " mm");
        return value.toString();
    }

    static String processNames(SettlePrintLineVO line) {
        Set<String> names = processFees(line).stream()
                .map(SettleFeeLineVO::getFeeName)
                .filter(name -> name != null && !name.isBlank())
                .map(SettleBillLineText::withoutFeeSuffix)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return names.isEmpty() ? text(line.getProcessText()) : String.join(" / ", names);
    }

    static String feeBasis(SettlePrintLineVO line) {
        List<SettleFeeLineVO> fees = processFees(line);
        if (fees.isEmpty()) {
            return firstText(line.getProcessStepSummary(), line.getProcessText());
        }
        return fees.stream().map(SettleBillLineText::feeText).collect(Collectors.joining("；"));
    }

    static String finishResult(SettlePrintLineVO line) {
        int finishCount = line.getFinishCount() == null ? 0 : line.getFinishCount();
        String result = finishCount + " 卷 / " + weightText(line.getFinishWeight());
        String summary = line.getFinishSummary();
        return summary == null || summary.isBlank() || "-".equals(summary) ? result : result + "（" + summary + "）";
    }

    static String trimText(SettlePrintLineVO line) {
        return firstText(line.getTrimSummary(), weightText(line.getTrimWeight()));
    }

    static String weightText(BigDecimal value) {
        return value == null ? "-" : text(value) + " kg";
    }

    static String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    static String text(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private static List<SettleFeeLineVO> processFees(SettlePrintLineVO line) {
        if (line.getFeeLines() == null) {
            return List.of();
        }
        return line.getFeeLines().stream()
                .filter(fee -> "saw".equals(fee.getFeeType()) || "rewind".equals(fee.getFeeType())
                        || "service".equals(fee.getFeeType()))
                .toList();
    }

    private static String feeText(SettleFeeLineVO fee) {
        String formula = firstText(fee.getFormulaText(), fee.getFeeName());
        if ((fee.getFormulaText() == null || fee.getFormulaText().isBlank())
                && fee.getQuantity() != null && fee.getUnitPrice() != null) {
            String unit = fee.getQuantityUnit() == null ? "" : fee.getQuantityUnit();
            formula = text(fee.getQuantity()) + unit + " × " + text(fee.getUnitPrice());
        }
        return fee.getAmountNoTax() == null ? formula : formula + " = " + text(fee.getAmountNoTax());
    }

    private static String withoutFeeSuffix(String name) {
        return name.endsWith("费") ? name.substring(0, name.length() - 1) : name;
    }

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : text(second);
    }

    private static void append(StringBuilder value, String item) {
        if (item != null && !item.isBlank()) {
            value.append(" / ").append(item);
        }
    }
}

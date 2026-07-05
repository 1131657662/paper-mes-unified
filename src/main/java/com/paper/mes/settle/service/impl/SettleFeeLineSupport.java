package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class SettleFeeLineSupport {

    private static final int OUTPUT_TYPE_INTERMEDIATE = 1;
    private static final int MONEY_SCALE = 2;
    private static final int WEIGHT_SCALE = 3;

    private SettleFeeLineSupport() {
    }

    static SettleFeeLineVO baseLine(String type, String name, SettlePrintLineVO line) {
        SettleFeeLineVO feeLine = new SettleFeeLineVO();
        feeLine.setFeeType(type);
        feeLine.setFeeName(name);
        feeLine.setSourceText(line.getOriginalLabel());
        feeLine.setOutputText(firstText(line.getFinishDetailSummary(), line.getFinishSummary()));
        return feeLine;
    }

    static void addFeeLine(SettlePrintLineVO line, SettleFeeLineVO feeLine) {
        List<SettleFeeLineVO> feeLines = line.getFeeLines();
        if (feeLines == null) {
            feeLines = new ArrayList<>();
            line.setFeeLines(feeLines);
        }
        feeLines.add(feeLine);
    }

    static String outputText(List<ProcessStageOutput> outputs, SettlePrintLineVO line) {
        if (outputs == null || outputs.isEmpty()) {
            return firstText(line.getFinishDetailSummary(), line.getFinishSummary(), "-");
        }
        return outputs.stream().map(SettleFeeLineSupport::outputLabel).reduce((left, right) -> left + "；" + right).orElse("-");
    }

    static String outputLabel(ProcessStageOutput output) {
        List<String> parts = new ArrayList<>();
        parts.add(firstText(output.getOutputNo(), "阶段产物"));
        if (StringUtils.hasText(output.getPaperName())) {
            parts.add(output.getPaperName());
        }
        if (output.getGramWeight() != null || output.getFinishWidth() != null) {
            parts.add(specText(output));
        }
        if (output.getEstimateWeight() != null) {
            parts.add(weight(output.getEstimateWeight()).stripTrailingZeros().toPlainString() + "kg");
        }
        parts.add(output.getOutputType() != null && output.getOutputType() == OUTPUT_TYPE_INTERMEDIATE ? "进入下道" : "最终成品");
        return String.join(" / ", parts);
    }

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "-";
    }

    static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }

    static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal weight(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal billingQuantity(BigDecimal amount, BigDecimal unitPrice, BigDecimal fallback) {
        if (!isZero(amount) && !isZero(unitPrice)) {
            return amount.divide(unitPrice, WEIGHT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return weight(fallback);
    }

    static String moneyText(BigDecimal value) {
        return money(value).stripTrailingZeros().toPlainString();
    }

    static String percentText(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString() + "%";
    }

    private static String specText(ProcessStageOutput output) {
        List<String> parts = new ArrayList<>();
        if (output.getGramWeight() != null) {
            parts.add(output.getGramWeight() + "g");
        }
        if (output.getFinishWidth() != null) {
            parts.add(output.getFinishWidth() + "mm");
        }
        if (output.getFinishDiameter() != null) {
            parts.add("φ" + output.getFinishDiameter());
        }
        if (output.getFinishCoreDiameter() != null) {
            parts.add("芯" + output.getFinishCoreDiameter());
        }
        return String.join(" / ", parts);
    }
}

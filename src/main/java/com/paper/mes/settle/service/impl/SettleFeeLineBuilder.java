package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SettleFeeLineBuilder {

    private static final int STEP_TYPE_SAW = 1;
    private static final int STEP_TYPE_REWIND = 2;
    private static final int INPUT_TYPE_STAGE_OUTPUT = 2;

    private SettleFeeLineBuilder() {
    }

    static List<SettleFeeLineVO> fromSteps(SettlePrintLineVO line, OriginalRoll roll,
                                           List<ProcessStep> steps, List<ProcessStageOutput> outputs) {
        List<SettleFeeLineVO> result = new ArrayList<>();
        Map<String, ProcessStageOutput> outputByUuid = outputByUuid(outputs);
        Map<String, List<ProcessStageOutput>> outputsByStep = outputsByStep(outputs);
        for (ProcessStep step : sortedSteps(steps)) {
            SettleFeeLineVO feeLine = fromStep(line, roll, step, outputByUuid, outputsByStep.get(step.getUuid()));
            if (feeLine != null) {
                result.add(feeLine);
            }
        }
        return result;
    }

    static void appendExtraLine(SettlePrintLineVO line, BigDecimal amount, String summary) {
        if (SettleFeeLineSupport.isZero(amount)) {
            return;
        }
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine("extra", "额外费用", line);
        feeLine.setAmountNoTax(SettleFeeLineSupport.money(amount));
        feeLine.setAmountTax(SettleFeeLineSupport.money(amount));
        feeLine.setFormulaText(StringUtils.hasText(summary) ? summary : "额外费用分摊 " + SettleFeeLineSupport.moneyText(amount));
        SettleFeeLineSupport.addFeeLine(line, feeLine);
    }

    static void appendTaxLine(SettlePrintLineVO line, BigDecimal amount) {
        if (SettleFeeLineSupport.isZero(amount)) {
            return;
        }
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine("tax", "开票加价", line);
        feeLine.setAmountNoTax(SettleFeeLineSupport.zeroMoney());
        feeLine.setTaxRate(line.getTaxRate());
        feeLine.setTaxAmount(SettleFeeLineSupport.money(amount));
        feeLine.setAmountTax(SettleFeeLineSupport.money(amount));
        feeLine.setFormulaText("按税点 " + SettleFeeLineSupport.percentText(line.getTaxRate()) + " 分摊开票加价");
        SettleFeeLineSupport.addFeeLine(line, feeLine);
    }

    static void ensureFeeLines(List<SettlePrintLineVO> lines) {
        SettleFeeLineFallbackBuilder.ensure(lines);
    }

    private static SettleFeeLineVO fromStep(SettlePrintLineVO line, OriginalRoll roll, ProcessStep step,
                                            Map<String, ProcessStageOutput> outputByUuid,
                                            List<ProcessStageOutput> outputs) {
        if (step.getStepType() == null) {
            return null;
        }
        if (step.getStepType() == STEP_TYPE_SAW) {
            return sawLine(line, roll, step, outputByUuid, outputs);
        }
        if (step.getStepType() == STEP_TYPE_REWIND) {
            return rewindLine(line, roll, step, outputByUuid, outputs);
        }
        return null;
    }

    private static SettleFeeLineVO sawLine(SettlePrintLineVO line, OriginalRoll roll, ProcessStep step,
                                           Map<String, ProcessStageOutput> outputByUuid,
                                           List<ProcessStageOutput> outputs) {
        SettleFeeLineVO feeLine = stepLine("saw", "锯纸费", line, roll, step, outputByUuid, outputs);
        BigDecimal quantity = step.getKnifeCount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(step.getKnifeCount());
        feeLine.setQuantity(quantity);
        feeLine.setQuantityUnit("刀");
        feeLine.setUnitPrice(step.getUnitPrice());
        feeLine.setAmountNoTax(SettleFeeLineSupport.money(step.getStepAmount()));
        feeLine.setAmountTax(SettleFeeLineSupport.money(step.getStepAmount()));
        feeLine.setFormulaText(quantity.stripTrailingZeros().toPlainString() + "刀 × "
                + SettleFeeLineSupport.moneyText(step.getUnitPrice()) + "元/刀");
        return feeLine;
    }

    private static SettleFeeLineVO rewindLine(SettlePrintLineVO line, OriginalRoll roll, ProcessStep step,
                                              Map<String, ProcessStageOutput> outputByUuid,
                                              List<ProcessStageOutput> outputs) {
        SettleFeeLineVO feeLine = stepLine("rewind", "复卷费", line, roll, step, outputByUuid, outputs);
        BigDecimal quantity = SettleFeeLineSupport.billingQuantity(step.getStepAmount(), step.getUnitPrice(), step.getProcessWeight());
        feeLine.setQuantity(quantity);
        feeLine.setQuantityUnit("t");
        feeLine.setUnitPrice(step.getUnitPrice());
        feeLine.setAmountNoTax(SettleFeeLineSupport.money(step.getStepAmount()));
        feeLine.setAmountTax(SettleFeeLineSupport.money(step.getStepAmount()));
        feeLine.setFormulaText(quantity.stripTrailingZeros().toPlainString() + "t × "
                + SettleFeeLineSupport.moneyText(step.getUnitPrice()) + "元/t");
        return feeLine;
    }

    private static SettleFeeLineVO stepLine(String type, String name, SettlePrintLineVO line, OriginalRoll roll,
                                            ProcessStep step, Map<String, ProcessStageOutput> outputByUuid,
                                            List<ProcessStageOutput> outputs) {
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine(type, name, line);
        feeLine.setStageLevel(step.getStageLevel());
        feeLine.setSourceText(sourceText(line, roll, step, outputByUuid));
        feeLine.setOutputText(SettleFeeLineSupport.outputText(outputs, line));
        feeLine.setRemark(step.getRemark());
        return feeLine;
    }

    private static String sourceText(SettlePrintLineVO line, OriginalRoll roll, ProcessStep step,
                                     Map<String, ProcessStageOutput> outputByUuid) {
        if (step.getInputType() != null && step.getInputType() == INPUT_TYPE_STAGE_OUTPUT) {
            ProcessStageOutput output = outputByUuid.get(step.getInputOutputUuid());
            if (output != null) {
                return SettleFeeLineSupport.outputLabel(output);
            }
        }
        return SettleFeeLineSupport.firstText(line.getOriginalLabel(), roll.getRollNo(), roll.getExtraNo(), "原卷");
    }

    private static Map<String, ProcessStageOutput> outputByUuid(List<ProcessStageOutput> outputs) {
        Map<String, ProcessStageOutput> result = new LinkedHashMap<>();
        for (ProcessStageOutput output : outputs) {
            if (StringUtils.hasText(output.getUuid())) {
                result.put(output.getUuid(), output);
            }
        }
        return result;
    }

    private static Map<String, List<ProcessStageOutput>> outputsByStep(List<ProcessStageOutput> outputs) {
        Map<String, List<ProcessStageOutput>> result = new LinkedHashMap<>();
        for (ProcessStageOutput output : outputs) {
            if (StringUtils.hasText(output.getStepUuid())) {
                result.computeIfAbsent(output.getStepUuid(), ignored -> new ArrayList<>()).add(output);
            }
        }
        return result;
    }

    private static List<ProcessStep> sortedSteps(List<ProcessStep> steps) {
        return steps.stream()
                .sorted(Comparator.comparing(ProcessStep::getStageLevel, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProcessStep::getStepSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProcessStep::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}

package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SawPlanPreviewer {

    private static final String ITEM_FINISH = "FINISH";
    private static final String ITEM_TRIM = "TRIM";

    private final SawPlanCalculator calculator = new SawPlanCalculator();

    public PlanPreviewVO preview(ProcessPlanDTO plan, OriginalRoll roll) {
        SawPlanCalculation calculation;
        try {
            calculation = calculator.calculate(plan.getFinishSpecs(), roll, plan.getWidthDifferencePolicy());
        } catch (BusinessException ex) {
            return errorPreview(plan, roll, ex.getMessage());
        }
        PlanPreviewVO result = shell(plan, roll, calculation);
        result.setFinishes(previewRows(calculation));
        result.setTotalEstimateWeight(totalEstimateWeight(calculation));
        result.setReady(true);
        result.setSummary(summary(calculation));
        return result;
    }

    public List<FinishConfigSpecDTO> saveSpecs(List<FinishConfigSpecDTO> specs, OriginalRoll roll,
                                                String policyValue) {
        SawPlanCalculation calculation = calculator.calculate(specs, roll, policyValue);
        List<FinishConfigSpecDTO> result = new ArrayList<>();
        calculation.finishes().forEach(item -> result.add(toSaveSpec(item)));
        if (calculation.policy() == WidthDifferencePolicy.REMAINDER && calculation.differenceWidth() > 0) {
            result.add(toRemainderSpec(calculation));
        }
        return result;
    }

    /** Backward-compatible entry point: historical plans represented unused width as inventory remainder. */
    public List<FinishConfigSpecDTO> saveSpecs(List<FinishConfigSpecDTO> specs, OriginalRoll roll) {
        if (Integer.valueOf(2).equals(roll.getProcessMode())) {
            return onSiteSpecs(specs);
        }
        return saveSpecs(specs, roll, WidthDifferencePolicy.REMAINDER.name());
    }

    public List<FinishConfigSpecDTO> finishSpecs(List<FinishConfigSpecDTO> specs) {
        return (specs == null ? List.<FinishConfigSpecDTO>of() : specs).stream()
                .filter(spec -> !ITEM_TRIM.equalsIgnoreCase(spec.getItemType()))
                .toList();
    }

    public SawPlanCalculation calculate(List<FinishConfigSpecDTO> specs, OriginalRoll roll, String policyValue) {
        return calculator.calculate(specs, roll, policyValue);
    }

    private PlanPreviewVO shell(ProcessPlanDTO plan, OriginalRoll roll, SawPlanCalculation calculation) {
        PlanPreviewVO result = new PlanPreviewVO();
        result.setOriginalUuid(roll.getUuid());
        result.setProcessMode(plan.getProcessMode());
        result.setMainStepType(plan.getMainStepType());
        result.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        result.setFinishCount(calculation.finishes().size());
        result.setTrimCount(calculation.policy() == WidthDifferencePolicy.REMAINDER
                && calculation.differenceWidth() > 0 ? 1 : 0);
        result.setTotalTrimWeight(balanceAdjustmentWeight(calculation));
        result.setWidthDifferencePolicy(calculation.policy().name());
        result.setWidthDifference(calculation.differenceWidth());
        result.setWidthDifferenceWeight(calculation.differenceWeight());
        result.setCalculatedLossWeight(calculation.policy() == WidthDifferencePolicy.LOSS
                ? calculation.differenceWeight() : BigDecimal.ZERO);
        return result;
    }

    private PlanPreviewVO errorPreview(ProcessPlanDTO plan, OriginalRoll roll, String message) {
        PlanPreviewVO result = new PlanPreviewVO();
        result.setOriginalUuid(roll.getUuid());
        result.setProcessMode(plan.getProcessMode());
        result.setMainStepType(plan.getMainStepType());
        result.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        result.setReady(false);
        result.getErrors().add(message);
        return result;
    }

    private List<FinishConfigSpecDTO> onSiteSpecs(List<FinishConfigSpecDTO> specs) {
        List<FinishConfigSpecDTO> result = new ArrayList<>();
        for (FinishConfigSpecDTO spec : finishSpecs(specs)) {
            for (int index = 0; index < (spec.getCount() == null ? 1 : spec.getCount()); index++) {
                FinishConfigSpecDTO row = new FinishConfigSpecDTO();
                row.setItemType(ITEM_FINISH);
                row.setCount(1);
                row.setFinishWidth(spec.getFinishWidth());
                row.setEstimateWeight(BigDecimal.ZERO.setScale(3));
                result.add(row);
            }
        }
        return result;
    }

    private List<FinishPreviewVO.FinishItemPreview> previewRows(SawPlanCalculation calculation) {
        List<FinishPreviewVO.FinishItemPreview> result = new ArrayList<>();
        for (SawPlanCalculation.CalculatedFinish item : calculation.finishes()) {
            FinishConfigSpecDTO spec = item.specification();
            FinishPreviewVO.FinishItemPreview row = new FinishPreviewVO.FinishItemPreview();
            row.setFinishWidth(spec.getFinishWidth());
            row.setFinishDiameter(spec.getFinishDiameter());
            row.setFinishCoreDiameter(spec.getFinishCoreDiameter());
            row.setCustomerPaperName(spec.getCustomerPaperName());
            row.setCustomerGramWeight(spec.getCustomerGramWeight());
            row.setCustomerFinishWidth(spec.getCustomerFinishWidth());
            row.setCustomerSpecOverrideReason(spec.getCustomerSpecOverrideReason());
            row.setEstimateWeight(item.estimateWeight());
            row.setTrimWidth(calculation.differenceWidth());
            row.setTrimWeight(calculation.differenceWeight());
            row.setSourceSummary("当前母卷");
            result.add(row);
        }
        return result;
    }

    private FinishConfigSpecDTO toSaveSpec(SawPlanCalculation.CalculatedFinish item) {
        FinishConfigSpecDTO source = item.specification();
        FinishConfigSpecDTO result = new FinishConfigSpecDTO();
        result.setItemType(ITEM_FINISH);
        result.setCount(1);
        result.setFinishWidth(source.getFinishWidth());
        result.setFinishDiameter(source.getFinishDiameter());
        result.setFinishCoreDiameter(source.getFinishCoreDiameter());
        result.setCustomerPaperName(source.getCustomerPaperName());
        result.setCustomerGramWeight(source.getCustomerGramWeight());
        result.setCustomerFinishWidth(source.getCustomerFinishWidth());
        result.setCustomerSpecOverrideReason(source.getCustomerSpecOverrideReason());
        result.setEstimateWeight(item.estimateWeight());
        return result;
    }

    private FinishConfigSpecDTO toRemainderSpec(SawPlanCalculation calculation) {
        FinishConfigSpecDTO result = new FinishConfigSpecDTO();
        result.setItemType(ITEM_TRIM);
        result.setCount(1);
        result.setFinishWidth(calculation.differenceWidth());
        result.setEstimateWeight(calculation.differenceWeight());
        return result;
    }

    private BigDecimal totalEstimateWeight(SawPlanCalculation calculation) {
        return calculation.finishes().stream()
                .map(SawPlanCalculation.CalculatedFinish::estimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal balanceAdjustmentWeight(SawPlanCalculation calculation) {
        return calculation.policy() == WidthDifferencePolicy.ALLOCATE
                ? BigDecimal.ZERO.setScale(3)
                : calculation.differenceWeight();
    }

    private String summary(SawPlanCalculation calculation) {
        return "锯纸预计生成 " + calculation.finishes().size() + " 个成品，刀数 "
                + calculation.knifeCount() + "，成品门幅 " + calculation.finishWidth()
                + "mm，门幅差额 " + calculation.differenceWidth() + "mm（"
                + policyLabel(calculation.policy()) + "）";
    }

    private String policyLabel(WidthDifferencePolicy policy) {
        if (policy == WidthDifferencePolicy.LOSS) return "计损耗";
        if (policy == WidthDifferencePolicy.ALLOCATE) return "分摊入成品";
        return "留余料";
    }
}

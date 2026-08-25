package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderOddWeightRegressionTest {

    @Test
    void splitsOddRewindWeightAcrossTwoSameSpecificationOutputs() {
        ProcessOrderDetailVO.RollProductionVO production = production("1501", 1550,
                output("rewind-a", 1550), output("rewind-b", 1550));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("rewind-a", decimal("751"))
                .containsEntry("rewind-b", decimal("750"));
        assertThat(weights.values()).containsOnly(decimal("751"), decimal("750"));
    }

    @Test
    void keepsSavedOddRewindPlanInsteadOfRecalculatingBothOutputs() {
        ProcessOrderDetailVO.StageOutputVO first = output("rewind-a", 1550);
        first.setEstimateWeight(decimal("751"));
        first.setWeightStatus("ESTIMATED");
        ProcessOrderDetailVO.StageOutputVO second = output("rewind-b", 1550);
        second.setEstimateWeight(decimal("750"));
        second.setWeightStatus("ESTIMATED");
        ProcessOrderDetailVO.RollProductionVO production = production("1501", 1550, first, second);

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("rewind-a", decimal("751"))
                .containsEntry("rewind-b", decimal("750"));
    }

    private ProcessOrderDetailVO.RollProductionVO production(
            String weight, int width, ProcessOrderDetailVO.StageOutputVO... outputs) {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setRollWeight(decimal(weight));
        production.setPieceNum(1);
        production.setOriginalWidth(width);
        ProcessStep rewind = new ProcessStep();
        rewind.setStageLevel(1);
        rewind.setStepType(2);
        rewind.setIsMain(1);
        production.setSteps(List.of(rewind));
        production.setStageOutputs(List.of(outputs));
        return production;
    }

    private ProcessOrderDetailVO.StageOutputVO output(String uuid, int width) {
        ProcessOrderDetailVO.StageOutputVO output = new ProcessOrderDetailVO.StageOutputVO();
        output.setUuid(uuid);
        output.setStageLevel(1);
        output.setFinishWidth(width);
        output.setSourceStepType(2);
        return output;
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}

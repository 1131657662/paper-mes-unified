package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.WorkshopInstructionVO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkshopInstructionBuilderTest {

    @Test
    void groupsEquivalentRowsAndSumsSourcePieces() {
        List<WorkshopInstructionVO> result = WorkshopInstructionBuilder.build(List.of(
                saw(1, 1, 900, 100), saw(2, 1, 900, 100), saw(3, 1, 900, 100)));

        assertThat(result).singleElement().satisfies(instruction -> {
            assertThat(instruction.sourceRows()).containsExactly(1, 2, 3);
            assertThat(instruction.sourcePieceCount()).isEqualTo(3);
            assertThat(instruction.text()).isEqualTo(
                    "1000mm母卷（R1-R3），共3件：锯纸；每件成品900mm；每件切边余料100mm。");
        });
    }

    @Test
    void countsOneSourceRowWithNinePiecesAsNinePieces() {
        ProcessOrderDetailVO.RollProductionVO production = saw(1, 9, 900, 100);
        production.setFinishes(repeatedFinishes(9, 900, false));

        List<WorkshopInstructionVO> result = WorkshopInstructionBuilder.build(List.of(production));

        assertThat(result.getFirst().sourcePieceCount()).isEqualTo(9);
        assertThat(result.getFirst().text()).contains("共9件").contains("每件成品900mm");
    }

    @Test
    void includesExplicitSawTrimWithoutDoubleCountingPlannedLoss() {
        ProcessOrderDetailVO.RollProductionVO production = saw(1, 2, 900, 0);
        List<ProcessOrderDetailVO.FinishProductionVO> finishes = new ArrayList<>();
        finishes.addAll(repeatedFinishes(2, 900, false));
        finishes.addAll(repeatedFinishes(2, 100, true));
        production.setFinishes(finishes);

        assertThat(WorkshopInstructionBuilder.build(List.of(production)).getFirst().text())
                .contains("每件切边余料100mm");
    }

    @Test
    void formatsWeightSplitRewindFromSavedParameters() {
        ProcessOrderDetailVO.RollProductionVO production = base(4, 1, 2000, 2);
        production.setRewindParams(List.of(
                rewindParam(2, 1000, 1200, 3, "50"),
                rewindParam(2, 1000, 1200, 3, "50")));

        assertThat(WorkshopInstructionBuilder.build(List.of(production)).getFirst().text())
                .isEqualTo("2000mm母卷（R4），共1件：复卷；每件按重量50%+50%分2卷；"
                        + "成品门幅1000mm×2；目标直径1200mm；纸芯3英寸。");
    }

    @Test
    void convertsLegacyInchDiameterBeforePrintingMillimetres() {
        ProcessOrderDetailVO.RollProductionVO production = base(4, 1, 2000, 2);
        production.setRewindParams(List.of(rewindParam(2, 1000, 3, 3, "100")));

        assertThat(WorkshopInstructionBuilder.build(List.of(production)).getFirst().text())
                .contains("76.2mm")
                .doesNotContain("目标直径3mm");
    }

    private ProcessOrderDetailVO.RollProductionVO saw(int row, int pieces, int width, int lossWidth) {
        ProcessOrderDetailVO.RollProductionVO production = base(row, pieces, 1000, 1);
        production.setFinishes(repeatedFinishes(pieces, width, false));
        ProcessStep step = new ProcessStep();
        step.setIsMain(1);
        step.setStepType(1);
        step.setPlannedLossWidth(lossWidth);
        production.setSteps(List.of(step));
        return production;
    }

    private ProcessOrderDetailVO.RollProductionVO base(int row, int pieces, int width, int stepType) {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setRowSort(row);
        production.setPieceNum(pieces);
        production.setOriginalWidth(width);
        production.setMainStepType(stepType);
        production.setSteps(List.of());
        production.setFinishes(List.of());
        production.setRewindParams(List.of());
        return production;
    }

    private List<ProcessOrderDetailVO.FinishProductionVO> repeatedFinishes(
            int count, int width, boolean remain) {
        List<ProcessOrderDetailVO.FinishProductionVO> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ProcessOrderDetailVO.FinishProductionVO finish = new ProcessOrderDetailVO.FinishProductionVO();
            finish.setFinishWidth(width);
            finish.setIsRemain(remain ? 1 : 0);
            result.add(finish);
        }
        return result;
    }

    private ProcessOrderDetailVO.RewindParamVO rewindParam(
            int mode, int width, int diameter, int core, String ratio) {
        ProcessOrderDetailVO.RewindParamVO param = new ProcessOrderDetailVO.RewindParamVO();
        param.setParamMode(mode);
        param.setLayerWidth(width);
        param.setOutDiameter(diameter);
        param.setCoreDiameter(core);
        param.setSplitRatio(new BigDecimal(ratio));
        return param;
    }
}

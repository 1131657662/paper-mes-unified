package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.entity.ProcessOrderAppendRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderAppendRollChangePolicyTest {

    @Test
    void sourceChanged_whenDecimalScaleDiffers_preservesSavedPlan() {
        ProcessOrderAppendRoll current = roll("100.000", 2, 1, 2);
        OriginalRollDTO requested = request("100", 2, 1, 2);

        boolean changed = ProcessOrderAppendRollChangePolicy.sourceChanged(current, requested);

        assertThat(changed).isFalse();
    }

    @Test
    void sourceChanged_whenWeightValueChanges_invalidatesSavedPlan() {
        ProcessOrderAppendRoll current = roll("100.000", 2, 1, 2);
        OriginalRollDTO requested = request("101", 2, 1, 2);

        boolean changed = ProcessOrderAppendRollChangePolicy.sourceChanged(current, requested);

        assertThat(changed).isTrue();
    }

    @Test
    void planInvalidated_whenOnlyRemarkChanges_preservesSavedPlan() {
        ProcessOrderAppendRoll current = roll("100.000", 2, 1, 2);
        current.setRemark("旧备注");
        OriginalRollDTO requested = request("100", 2, 1, 2);
        requested.setRemark("新备注");

        boolean invalidated = ProcessOrderAppendRollChangePolicy.planInvalidated(current, requested);

        assertThat(invalidated).isFalse();
    }

    @Test
    void planInvalidated_whenMainProcessChanges_invalidatesSavedPlan() {
        ProcessOrderAppendRoll current = roll("100.000", 2, 1, 2);
        OriginalRollDTO requested = request("100", 2, 1, 1);

        boolean invalidated = ProcessOrderAppendRollChangePolicy.planInvalidated(current, requested);

        assertThat(invalidated).isTrue();
    }

    private ProcessOrderAppendRoll roll(String weight, int pieces, int mode, int mainStep) {
        ProcessOrderAppendRoll roll = new ProcessOrderAppendRoll();
        roll.setPaperName("测试纸");
        roll.setGramWeight(80);
        roll.setOriginalWidth(1000);
        roll.setOriginalDiameter(1200);
        roll.setCoreDiameter(3);
        roll.setOriginalLength(5000);
        roll.setRollWeight(new BigDecimal(weight));
        roll.setPieceNum(pieces);
        roll.setProcessMode(mode);
        roll.setMainStepType(mainStep);
        roll.setMachineUuid("machine-1");
        return roll;
    }

    private OriginalRollDTO request(String weight, int pieces, int mode, int mainStep) {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setPaperName("测试纸");
        dto.setGramWeight(80);
        dto.setOriginalWidth(1000);
        dto.setOriginalDiameter(1200);
        dto.setCoreDiameter(3);
        dto.setOriginalLength(5000);
        dto.setRollWeight(new BigDecimal(weight));
        dto.setPieceNum(pieces);
        dto.setProcessMode(mode);
        dto.setMainStepType(mainStep);
        dto.setMachineUuid("machine-1");
        return dto;
    }
}

package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessOrderPrintableConfigValidatorTest {

    @Test
    void mergedFinish_withBothSourceRelations_allowsBothMotherRolls() {
        OriginalRoll first = roll("roll-1", "R001");
        OriginalRoll second = roll("roll-2", "R002");
        FinishRoll finish = finish("finish-1", "R001");

        assertThatCode(() -> ProcessOrderPrintableConfigValidator.validate(
                List.of(first, second), List.of(finish),
                List.of(step("roll-1"), step("roll-2")),
                List.of(relation("finish-1", "roll-1"), relation("finish-1", "roll-2"))))
                .doesNotThrowAnyException();
    }

    @Test
    void motherRoll_withoutFormalFinishOrSourceRelation_rejectsPrint() {
        OriginalRoll first = roll("roll-1", "R001");
        OriginalRoll second = roll("roll-2", "R002");

        assertThatThrownBy(() -> ProcessOrderPrintableConfigValidator.validate(
                List.of(first, second), List.of(finish("finish-1", "R001")),
                List.of(step("roll-1"), step("roll-2")),
                List.of(relation("finish-1", "roll-1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("R002");
    }

    private OriginalRoll roll(String uuid, String rollNo) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRollNo(rollNo);
        roll.setProcessMode(1);
        return roll;
    }

    private ProcessStep step(String originalUuid) {
        ProcessStep step = new ProcessStep();
        step.setOriginalUuid(originalUuid);
        step.setIsMain(1);
        return step;
    }

    private FinishRoll finish(String uuid, String originalRollNos) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setOriginalRollNos(originalRollNos);
        finish.setIsSpare(0);
        finish.setIsRemain(0);
        finish.setRollNoStatus(1);
        return finish;
    }

    private FinishOriginalRel relation(String finishUuid, String originalUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(originalUuid);
        return relation;
    }
}

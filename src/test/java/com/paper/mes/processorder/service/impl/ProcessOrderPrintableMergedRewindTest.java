package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessOrderPrintableMergedRewindTest {

    @Test
    void mergedRewind_withOwnerMainStep_allowsSourceWithoutDuplicateMainStep() {
        OriginalRoll source = roll("roll-2", "R002", 2);

        assertThatCode(() -> validate(source, step(2), List.of(param())))
                .doesNotThrowAnyException();
    }

    @Test
    void multiSourceFinish_withoutMergedRewindParam_stillRejectsMissingMainStep() {
        assertThatThrownBy(() -> validate(roll("roll-2", "R002", 2), step(2), List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("R002");
    }

    @Test
    void mergedRewind_withSawMainStep_stillRejectsMissingSourceMainStep() {
        assertThatThrownBy(() -> validate(roll("roll-2", "R002", 2), step(1), List.of(param())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("R002");
    }

    @Test
    void mergedRewind_sourceDeclaredAsSaw_stillRequiresOwnMainStep() {
        assertThatThrownBy(() -> validate(roll("roll-2", "R002", 1), step(2), List.of(param())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("R002");
    }

    @Test
    void mergedRewind_withScrappedFinish_stillRejectsMissingSourceMainStep() {
        FinishRoll scrapped = finish();
        scrapped.setFinishStatus(4);

        assertThatThrownBy(() -> ProcessOrderPrintableConfigValidator.validate(
                List.of(roll("roll-1", "R001", 2), roll("roll-2", "R002", 2)), List.of(scrapped),
                evidence(step(2), List.of(param()), List.of(relation("roll-1"), relation("roll-2")))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("R001");
    }

    @Test
    void mergedRewind_withDuplicateOwnerRelations_doesNotCreateMultiSourceCoverage() {
        OriginalRoll owner = roll("roll-1", "R001", 2);
        OriginalRoll source = roll("roll-2", "R002", 2);
        List<FinishOriginalRel> duplicates = List.of(relation("roll-1"), relation("roll-1"));

        assertThatThrownBy(() -> ProcessOrderPrintableConfigValidator.validate(
                List.of(owner, source), List.of(finish()), evidence(step(2), List.of(param()), duplicates)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("R002");
    }

    private void validate(OriginalRoll source, ProcessStep ownerStep, List<ProcessParam> params) {
        ProcessOrderPrintableConfigValidator.validate(
                List.of(roll("roll-1", "R001", 2), source), List.of(finish()),
                evidence(ownerStep, params, List.of(relation("roll-1"), relation("roll-2"))));
    }

    private OriginalRoll roll(String uuid, String rollNo, int mainStepType) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRollNo(rollNo);
        roll.setProcessMode(1);
        roll.setMainStepType(mainStepType);
        return roll;
    }

    private ProcessStep step(int stepType) {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setOriginalUuid("roll-1");
        step.setIsMain(1);
        step.setStepType(stepType);
        return step;
    }

    private ProcessParam param() {
        ProcessParam param = new ProcessParam();
        param.setOriginalUuid("roll-1");
        param.setStepUuid("step-1");
        param.setParamMode(5);
        return param;
    }

    private FinishRoll finish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setOriginalRollNos("R001");
        finish.setIsSpare(0);
        finish.setIsRemain(0);
        finish.setRollNoStatus(1);
        return finish;
    }

    private FinishOriginalRel relation(String originalUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid("finish-1");
        relation.setOriginalUuid(originalUuid);
        return relation;
    }

    private ProcessOrderPrintableConfigValidator.ProcessEvidence evidence(
            ProcessStep step, List<ProcessParam> params, List<FinishOriginalRel> relations) {
        return new ProcessOrderPrintableConfigValidator.ProcessEvidence(List.of(step), params, relations);
    }
}

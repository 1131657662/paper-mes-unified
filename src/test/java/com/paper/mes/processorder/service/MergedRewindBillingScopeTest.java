package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MergedRewindBillingScopeTest {

    @Test
    void mergedFinish_suppressesOnlyNonOwnerRewindMainStep() {
        ProcessStep ownerStep = step("owner", "owner-step");
        ProcessStep sourceMain = step("source", "source-main");
        ProcessStep sourceExtra = step("source", "source-extra");
        sourceExtra.setIsMain(0);
        ProcessParam ownerParam = param("owner", "owner-step");
        FinishRoll finish = finish("finish");

        Set<String> suppressed = MergedRewindBillingScope.suppressedMainStepUuids(
                new MergedRewindBillingScope.Evidence(
                        List.of(ownerStep, sourceMain, sourceExtra), List.of(ownerParam), List.of(finish),
                        List.of(relation("finish", "owner"), relation("finish", "source"))));

        assertThat(suppressed).containsExactly("source-main");
    }

    @Test
    void singleSourceFinish_doesNotSuppressItsOwner() {
        Set<String> suppressed = MergedRewindBillingScope.suppressedMainStepUuids(
                new MergedRewindBillingScope.Evidence(
                        List.of(step("owner", "owner-step")), List.of(param("owner", "owner-step")),
                        List.of(finish("finish")), List.of(relation("finish", "owner"))));

        assertThat(suppressed).isEmpty();
    }

    private ProcessStep step(String rollUuid, String stepUuid) {
        ProcessStep step = new ProcessStep();
        step.setOriginalUuid(rollUuid);
        step.setUuid(stepUuid);
        step.setIsMain(1);
        step.setStepType(2);
        return step;
    }

    private ProcessParam param(String rollUuid, String stepUuid) {
        ProcessParam param = new ProcessParam();
        param.setOriginalUuid(rollUuid);
        param.setStepUuid(stepUuid);
        param.setParamMode(5);
        return param;
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setIsSpare(0);
        finish.setIsRemain(0);
        finish.setRollNoStatus(1);
        return finish;
    }

    private FinishOriginalRel relation(String finishUuid, String rollUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(rollUuid);
        return relation;
    }
}

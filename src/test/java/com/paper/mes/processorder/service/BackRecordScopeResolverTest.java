package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackRecordScopeResolverTest {

    private final BackRecordScopeResolver resolver = new BackRecordScopeResolver();

    @Test
    void resolve_singleRoll_includesOnlyItsClosureData() {
        OriginalRoll first = roll("roll-1", 0);
        OriginalRoll second = roll("roll-2", 0);
        FinishRoll firstFinish = finish("finish-1");
        FinishRoll secondFinish = finish("finish-2");

        BackRecordScope scope = resolver.resolve(
                List.of(first, second),
                List.of(firstFinish, secondFinish),
                List.of(step("step-1", "roll-1"), step("step-2", "roll-2")),
                List.of(relation("finish-1", "roll-1"), relation("finish-2", "roll-2")),
                request("roll-1"));

        assertThat(scope.rolls()).extracting(OriginalRoll::getUuid).containsExactly("roll-1");
        assertThat(scope.finishes()).extracting(FinishRoll::getUuid).containsExactly("finish-1");
        assertThat(scope.steps()).extracting(ProcessStep::getUuid).containsExactly("step-1");
    }

    @Test
    void resolve_partialMergeGroup_rejectsSplitSubmission() {
        assertThatThrownBy(() -> resolver.resolve(
                List.of(roll("roll-1", 0), roll("roll-2", 0)),
                List.of(finish("finish-1")),
                List.of(),
                List.of(relation("finish-1", "roll-1"), relation("finish-1", "roll-2")),
                request("roll-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同一批");
    }

    @Test
    void resolve_alreadyRecordedRoll_rejectsOverwrite() {
        assertThatThrownBy(() -> resolver.resolve(
                List.of(roll("roll-1", 1)), List.of(), List.of(), List.of(), request("roll-1")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E004.getCode());
    }

    @Test
    void resolve_partialBatchSelectingEveryRemainingRoll_requiresOrderCompletion() {
        BackRecordDTO dto = request("roll-2");
        dto.setCompleteOrder(false);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(roll("roll-1", 1), roll("roll-2", 0)),
                List.of(), List.of(), List.of(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("完成整单");
    }

    @Test
    void resolve_completeOrderLeavingAnUnrecordedRoll_rejectsSubmission() {
        BackRecordDTO dto = request("roll-1");
        dto.setCompleteOrder(true);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(roll("roll-1", 0), roll("roll-2", 0)),
                List.of(), List.of(), List.of(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("全部未回录母卷");
    }

    @Test
    void resolve_unlinkedFinishWithExplicitSource_includesItInSelectedBatch() {
        BackRecordDTO dto = request("roll-1");
        dto.setCompleteOrder(false);
        BackRecordFinishDTO finishRow = new BackRecordFinishDTO();
        finishRow.setUuid("finish-1");
        finishRow.setOriginalUuid("roll-1");
        dto.setFinishes(List.of(finishRow));

        BackRecordScope scope = resolver.resolve(
                List.of(roll("roll-1", 0), roll("roll-2", 0)),
                List.of(finish("finish-1")), List.of(), List.of(), dto);

        assertThat(scope.finishes()).extracting(FinishRoll::getUuid).containsExactly("finish-1");
    }

    private BackRecordDTO request(String rollUuid) {
        BackRecordRollDTO row = new BackRecordRollDTO();
        row.setUuid(rollUuid);
        BackRecordDTO dto = new BackRecordDTO();
        dto.setCompleteOrder(false);
        dto.setRolls(List.of(row));
        return dto;
    }

    private OriginalRoll roll(String uuid, int checked) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setIsChecked(checked);
        return roll;
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        return finish;
    }

    private ProcessStep step(String uuid, String rollUuid) {
        ProcessStep step = new ProcessStep();
        step.setUuid(uuid);
        step.setOriginalUuid(rollUuid);
        return step;
    }

    private FinishOriginalRel relation(String finishUuid, String rollUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(rollUuid);
        return relation;
    }
}

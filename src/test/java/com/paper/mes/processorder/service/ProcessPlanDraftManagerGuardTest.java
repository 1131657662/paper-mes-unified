package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPlanDraftManagerGuardTest {

    private ProcessPlanDraftManagerFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ProcessPlanDraftManagerFixture();
    }

    @Test
    void saveProcessPlan_withExistingRouteDraft_rejectsWithoutWriting() {
        OriginalRoll roll = fixture.roll("roll-1", 1, 2);
        fixture.useRolls(roll);
        when(fixture.draftMapper.selectOne(any())).thenReturn(fixture.routeDraft("roll-1"));

        assertThatThrownBy(() -> fixture.manager.saveProcessPlan(
                "order-1", "roll-1", fixture.plan(1, 2), 7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链式工艺");

        verifyNoWrites();
    }

    @Test
    void saveProcessPlan_withMismatchedMode_rejectsWithoutWriting() {
        fixture.useRolls(fixture.roll("roll-1", 1, 2));

        assertThatThrownBy(() -> fixture.manager.saveProcessPlan(
                "order-1", "roll-1", fixture.plan(2, 2), 7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不一致");

        verifyNoWrites();
    }

    @Test
    void saveProcessPlan_withMismatchedMainStep_rejectsWithoutWriting() {
        fixture.useRolls(fixture.roll("roll-1", 1, 2));

        assertThatThrownBy(() -> fixture.manager.saveProcessPlan(
                "order-1", "roll-1", fixture.plan(1, 1), 7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不一致");

        verifyNoWrites();
    }

    @Test
    void saveProcessPlan_withMatchingMode_updatesOnlyMachineSelection() {
        OriginalRoll roll = fixture.roll("roll-1", 1, 2);
        fixture.useRolls(roll);
        ProcessPlanDTO plan = fixture.plan(1, 2);
        plan.setMachineUuid("machine-2");

        fixture.manager.saveProcessPlan("order-1", "roll-1", plan, 7);

        ArgumentCaptor<OriginalRoll> rollCaptor = ArgumentCaptor.forClass(OriginalRoll.class);
        verify(fixture.rollMapper).updateById(rollCaptor.capture());
        assertThat(rollCaptor.getValue().getProcessMode()).isEqualTo(1);
        assertThat(rollCaptor.getValue().getMainStepType()).isEqualTo(2);
        assertThat(rollCaptor.getValue().getMachineUuid()).isEqualTo("machine-2");
        verify(fixture.versionGuard).advance("order-1", 7);
        verify(fixture.draftMapper).insert(any(ProcessConfigDraft.class));
    }

    private void verifyNoWrites() {
        verify(fixture.versionGuard, never()).advance(any(), any());
        verify(fixture.rollMapper, never()).updateById(any(OriginalRoll.class));
        verify(fixture.draftMapper, never()).insert(any(ProcessConfigDraft.class));
        verify(fixture.draftMapper, never()).updateById(any(ProcessConfigDraft.class));
    }
}

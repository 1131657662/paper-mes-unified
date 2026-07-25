package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPlanDraftManagerBatchGuardTest {

    private ProcessPlanDraftManagerFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ProcessPlanDraftManagerFixture();
    }

    @Test
    void saveBatch_withDuplicateRoll_rejectsBeforeLoadingOrWriting() {
        assertThatThrownBy(() -> fixture.manager.saveBatch(
                "order-1", fixture.batch(fixture.plan(1, 2), "roll-1", "roll-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复");

        verify(fixture.rollMapper, never()).selectBatchIds(anyCollection());
        verifyNoWrites();
    }

    @Test
    void saveItemsBatch_withDuplicateRoll_rejectsBeforeLoadingOrWriting() {
        assertThatThrownBy(() -> fixture.manager.saveItemsBatch(
                "order-1", fixture.items(fixture.plan(1, 2), "roll-1", "roll-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复");

        verify(fixture.rollMapper, never()).selectBatchIds(anyCollection());
        verifyNoWrites();
    }

    @Test
    void saveBatch_withMultiSourcePlan_rejectsWithoutWriting() {
        ProcessPlanDTO plan = fixture.plan(1, 2);
        plan.setRewindMode(5);

        assertThatThrownBy(() -> fixture.manager.saveBatch(
                "order-1", fixture.batch(plan, "roll-1", "roll-2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("多来源");

        verify(fixture.rollMapper, never()).selectBatchIds(anyCollection());
        verifyNoWrites();
    }

    @Test
    void saveBatch_withLaterModeMismatch_rejectsEntireBatchBeforePreviewOrWrite() {
        fixture.useRolls(
                fixture.roll("roll-1", 1, 2),
                fixture.roll("roll-2", 2, 2));

        assertThatThrownBy(() -> fixture.manager.saveBatch(
                "order-1", fixture.batch(fixture.plan(1, 2), "roll-1", "roll-2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不一致");

        verify(fixture.previewer, never()).preview(any(), any(), any());
        verifyNoWrites();
    }

    @Test
    void saveItemsBatch_withLaterModeMismatch_rejectsEntireBatchBeforePreviewOrWrite() {
        fixture.useRolls(
                fixture.roll("roll-1", 1, 2),
                fixture.roll("roll-2", 2, 2));

        assertThatThrownBy(() -> fixture.manager.saveItemsBatch(
                "order-1", fixture.items(fixture.plan(1, 2), "roll-1", "roll-2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不一致");

        verify(fixture.previewer, never()).preview(any(), any(), any());
        verifyNoWrites();
    }

    @Test
    void saveBatch_withLaterRouteDraft_rejectsEntireBatchBeforePreviewOrWrite() {
        OriginalRoll first = fixture.roll("roll-1", 1, 2);
        OriginalRoll second = fixture.roll("roll-2", 1, 2);
        fixture.useRolls(first, second);
        when(fixture.draftMapper.selectList(any())).thenReturn(List.of(fixture.routeDraft("roll-2")));

        assertThatThrownBy(() -> fixture.manager.saveBatch(
                "order-1", fixture.batch(fixture.plan(1, 2), "roll-1", "roll-2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链式工艺");

        verify(fixture.previewer, never()).preview(any(), any(), any());
        verifyNoWrites();
    }

    @Test
    void saveBatch_whenLaterPreviewFails_doesNotPersistEarlierTarget() {
        fixture.useRolls(
                fixture.roll("roll-1", 1, 2),
                fixture.roll("roll-2", 1, 2));
        doAnswer(invocation -> {
            OriginalRoll roll = invocation.getArgument(1);
            if ("roll-2".equals(roll.getUuid())) {
                throw new BusinessException("预览失败");
            }
            return new PlanPreviewVO();
        }).when(fixture.previewer).preview(any(), any(), any());

        assertThatThrownBy(() -> fixture.manager.saveBatch(
                "order-1", fixture.batch(fixture.plan(1, 2), "roll-1", "roll-2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览失败");

        verifyNoWrites();
    }

    private void verifyNoWrites() {
        verify(fixture.versionGuard, never()).advance(any(), any());
        verify(fixture.rollMapper, never()).updateById(any(OriginalRoll.class));
        verify(fixture.draftMapper, never()).insert(any(ProcessConfigDraft.class));
        verify(fixture.draftMapper, never()).updateById(any(ProcessConfigDraft.class));
    }
}

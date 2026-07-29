package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProcessDraftConcurrencyBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderDraftService draftService;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private OriginalRollMapper originalRollMapper;
    @Autowired private ProcessConfigDraftMapper draftMapper;

    private BackRecordOnSiteFixture.Scenario scenario;

    @BeforeEach
    void setUp() {
        scenario = fixture.arrange();
        scenario.order().setOrderStatus(0);
        processOrderMapper.updateById(scenario.order());
    }

    @AfterEach
    void tearDown() {
        cleanup.delete(scenario.order().getUuid());
    }

    @Test
    void processPlan_whenSavedConcurrently_keepsOneCompleteDraft() throws Exception {
        String orderUuid = scenario.order().getUuid();
        String rollUuid = scenario.roll().getUuid();
        int expectedVersion = processOrderMapper.selectById(orderUuid).getVersion();

        var outcomes = ConcurrentBusinessActions.<PlanPreviewVO>runPair(
                () -> draftService.saveProcessPlan(orderUuid, rollUuid, plan(1, "并发方案A"), expectedVersion),
                () -> draftService.saveProcessPlan(orderUuid, rollUuid, plan(1, "并发方案B"), expectedVersion));

        assertThat(outcomes).filteredOn(ConcurrentBusinessActions.Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded()).singleElement()
                .satisfies(outcome -> {
                    assertThat(outcome.error()).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) outcome.error()).getErrorCode())
                            .isEqualTo(ErrorCode.E006.getCode());
                });
        var drafts = draftMapper.selectList(new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid)
                .eq(ProcessConfigDraft::getOriginalUuid, rollUuid));
        assertThat(drafts).hasSize(1);
        assertThat(drafts.getFirst().getConfigJson())
                .containsAnyOf("并发方案A", "并发方案B");
        assertThat(originalRollMapper.selectById(rollUuid).getMainStepType()).isEqualTo(1);
        assertThat(processOrderMapper.selectById(orderUuid).getVersion()).isEqualTo(expectedVersion + 1);
    }

    private ProcessPlanDTO plan(int mainStepType, String remark) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(2);
        plan.setMainStepType(mainStepType);
        plan.setRemark(remark);
        return plan;
    }
}

package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.dto.BackRecordStepDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.ProcessRollDispositionDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import com.paper.mes.processorder.model.WeightEntryMode;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRollDispositionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class MultiSourceRewindBusinessFlowIT {

    @Autowired private RepresentativeRewindFixture rewindFixture;
    @Autowired private RepresentativeOrderFixture orderFixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessRollDispositionService processRollDispositionService;
    @Autowired private FinishOriginalRelMapper relationMapper;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void mergedRewind_withTwoMotherRolls_completesWithBothSources() {
        scenario = rewindFixture.createMerge();

        orderFixture.issueAndComplete(scenario);

        var detail = processOrderService.getDetail(scenario.orderUuid());
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getOriginalRolls()).hasSize(2);
        assertThat(detail.getFinishRolls()).hasSize(1);
        assertThat(detail.getRollProductions().getFirst().getFinishes().getFirst().getSources()).hasSize(2);
    }

    @Test
    void mergedRewind_withThreeUnknownMotherRolls_measuresAllSourcesAndRecalculatesFee() {
        scenario = rewindFixture.createUnknownMerge();
        processOrderService.issue(scenario.orderUuid());
        processOrderService.print(scenario.orderUuid(), new PrintDTO());
        processOrderService.completeProcessing(scenario.orderUuid(), "未知重量联调");
        var before = processOrderService.getDetail(scenario.orderUuid());

        processOrderService.backRecord(scenario.orderUuid(), request(before));

        var detail = processOrderService.getDetail(scenario.orderUuid());
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getOriginalRolls()).extracting(item -> item.getWeightStatus())
                .containsOnly("MEASURED");
        assertThat(detail.getOrder().getTotalProcessAmount()).isEqualByComparingTo("300.00");
        List<FinishOriginalRel> relations = relationMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOrderUuid, scenario.orderUuid()));
        assertThat(relations).extracting(FinishOriginalRel::getShareRatio)
                .containsExactlyInAnyOrder(new BigDecimal("30.00"), new BigDecimal("35.00"), new BigDecimal("35.00"));
    }

    @Test
    void mergedRewind_withKnownReferences_confirmsWithoutRetypingAndFinalizesFee() {
        scenario = rewindFixture.createMerge();
        processOrderService.issue(scenario.orderUuid());
        processOrderService.print(scenario.orderUuid(), new PrintDTO());
        processOrderService.completeProcessing(scenario.orderUuid(), "参考重量确认联调");
        var before = processOrderService.getDetail(scenario.orderUuid());

        processOrderService.backRecord(scenario.orderUuid(), referenceRequest(before));

        var detail = processOrderService.getDetail(scenario.orderUuid());
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getOriginalRolls()).allSatisfy(roll -> {
            assertThat(roll.getWeightStatus()).isEqualTo("MEASURED");
            assertThat(roll.getWeightSource()).isEqualTo("MANUAL_CONFIRM");
            assertThat(roll.getWeightRecordedAt()).isNotNull();
            assertThat(roll.getWeightRecordedBy()).isNotBlank();
        });
        assertThat(detail.getOrder().getTotalProcessAmount()).isEqualByComparingTo("240.00");
        assertThat(detail.getSteps()).filteredOn(step -> Integer.valueOf(2).equals(step.getStepType())
                        && !"FIXED".equalsIgnoreCase(step.getBillingWeightBasis()))
                .allSatisfy(step -> {
                    assertThat(step.getProcessWeight()).isEqualByComparingTo("1.600");
                    assertThat(step.getBillingWeightStatus()).isEqualTo("MEASURED");
                    assertThat(step.getPricingDirty()).isZero();
                });
    }

    @Test
    void mergedRewind_cannotDisposeOneSourceWhileSharedOutputIsActive() {
        scenario = rewindFixture.createMerge();
        processOrderService.issue(scenario.orderUuid());
        processOrderService.print(scenario.orderUuid(), new PrintDTO());
        processOrderService.completeProcessing(scenario.orderUuid(), "共享成品处置边界测试");
        var detail = processOrderService.getDetail(scenario.orderUuid());

        ProcessRollDispositionDTO command = new ProcessRollDispositionDTO();
        command.setAction(ProcessRollDispositionAction.CANCEL);
        command.setRequestId("shared-output-disposition-test");
        command.setReason("验证共享合并复卷不能拆解单个母卷");
        command.setExpectedOrderVersion(detail.getOrder().getVersion());

        assertThatThrownBy(() -> processRollDispositionService.dispose(
                detail.getOriginalRolls().getFirst().getUuid(), command))
                .hasMessageContaining("合并复卷");
    }

    private BackRecordDTO request(com.paper.mes.processorder.dto.ProcessOrderDetailVO detail) {
        BackRecordDTO dto = new BackRecordDTO();
        dto.setExpectedVersion(detail.getOrder().getVersion());
        dto.setCompleteOrder(true);
        dto.setWarehouseUuid(detail.getOrder().getWarehouseUuid());
        dto.setRolls(List.of(
                roll(detail.getOriginalRolls().get(0).getUuid(), "600"),
                roll(detail.getOriginalRolls().get(1).getUuid(), "700"),
                roll(detail.getOriginalRolls().get(2).getUuid(), "700")));
        dto.setFinishes(List.of(finish(detail.getFinishRolls().getFirst().getUuid(), "2000")));
        dto.setSteps(detail.getSteps().stream().map(step -> {
            BackRecordStepDTO record = new BackRecordStepDTO();
            record.setUuid(step.getUuid());
            record.setLossWeight(BigDecimal.ZERO);
            return record;
        }).toList());
        return dto;
    }

    private BackRecordDTO referenceRequest(com.paper.mes.processorder.dto.ProcessOrderDetailVO detail) {
        BackRecordDTO dto = new BackRecordDTO();
        dto.setExpectedVersion(detail.getOrder().getVersion());
        dto.setCompleteOrder(true);
        dto.setWarehouseUuid(detail.getOrder().getWarehouseUuid());
        dto.setRolls(detail.getOriginalRolls().stream().map(roll -> {
            BackRecordRollDTO record = roll(roll.getUuid(), roll.getTotalWeight().toPlainString());
            record.setWeightEntryMode(WeightEntryMode.CONFIRM_REFERENCE);
            return record;
        }).toList());
        dto.setFinishes(detail.getFinishRolls().stream()
                .map(finish -> finish(finish.getUuid(), finish.getEstimateWeight().toPlainString()))
                .toList());
        dto.setSteps(detail.getSteps().stream().map(step -> {
            BackRecordStepDTO record = new BackRecordStepDTO();
            record.setUuid(step.getUuid());
            record.setLossWeight(BigDecimal.ZERO);
            return record;
        }).toList());
        return dto;
    }

    private BackRecordRollDTO roll(String uuid, String weight) {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid(uuid);
        dto.setActualGramWeight(100);
        dto.setActualWidth(1500);
        dto.setActualWeight(new BigDecimal(weight));
        return dto;
    }

    private BackRecordFinishDTO finish(String uuid, String weight) {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setUuid(uuid);
        dto.setActualWeight(new BigDecimal(weight));
        dto.setIsRemain(0);
        dto.setIsAbnormal(0);
        return dto;
    }
}

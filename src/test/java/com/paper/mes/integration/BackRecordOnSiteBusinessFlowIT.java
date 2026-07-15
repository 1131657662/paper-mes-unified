package com.paper.mes.integration;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
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
class BackRecordOnSiteBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private BackRecordDynamicFinishFixture dynamicFixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private FinishRollMapper finishRollMapper;
    @Autowired private FinishOriginalRelMapper relationMapper;
    @Autowired private ProcessStepMapper processStepMapper;

    @Test
    void onSiteBackRecord_withoutFinishWidth_rejectsWithoutPersistingWeight() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = fixture.request(scenario, null, 800);

        assertThatThrownBy(() -> processOrderService.backRecord(scenario.order().getUuid(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("现场定尺成品门幅必须大于0");

        assertThat(finishRollMapper.selectById(scenario.first().getUuid()).getActualWeight()).isNull();
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(3);
    }

    @Test
    void onSiteBackRecord_withFinishWidths_completesInventoryAndSnapshot() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();

        processOrderService.backRecord(scenario.order().getUuid(), fixture.request(scenario, 900, 800));

        FinishRoll first = finishRollMapper.selectById(scenario.first().getUuid());
        FinishRoll spare = finishRollMapper.selectById(scenario.spare().getUuid());
        ProcessOrder order = processOrderMapper.selectById(scenario.order().getUuid());
        assertThat(first.getFinishWidth()).isEqualTo(900);
        assertThat(first.getFinishStatus()).isEqualTo(2);
        assertThat(first.getRollNoStatus()).isEqualTo(2);
        assertThat(spare.getRollNoStatus()).isEqualTo(3);
        assertThat(order.getOrderStatus()).isEqualTo(4);
        assertThat(order.getSnapFinish()).contains("\"finish_width\": 900");
        assertThat(relationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getFinishUuid, first.getUuid())).getFirst().getShareWeight())
                .isEqualByComparingTo("100.000");
    }

    @Test
    void onSiteBackRecord_withoutPreGeneratedFinishes_createsFormalFinish() {
        BackRecordOnSiteFixture.Scenario scenario = dynamicFixture.arrangeWithoutFinishes();
        assertThat(finishes(scenario.order()).isEmpty()).isTrue();

        processOrderService.backRecord(
                scenario.order().getUuid(), dynamicFixture.request(scenario));

        FinishRoll finish = finishes(scenario.order()).getFirst();
        assertThat(finish.getUuid()).isNotBlank();
        assertThat(finish.getFinishRollNo()).isNotBlank();
        assertThat(finish.getFinishWidth()).isEqualTo(900);
        assertThat(finish.getFinishStatus()).isEqualTo(2);
        assertThat(finish.getRollNoStatus()).isEqualTo(2);
        assertThat(relationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getFinishUuid, finish.getUuid())))
                .extracting(FinishOriginalRel::getOriginalUuid)
                .containsExactly(scenario.roll().getUuid());
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getSnapFinish())
                .contains(finish.getUuid())
                .contains("\"finish_width\": 900");
    }

    @Test
    void onSiteOrder_withoutPreGeneratedFinishes_canPrintAndIssue() {
        BackRecordOnSiteFixture.Scenario scenario = dynamicFixture.arrangeWithoutFinishes();
        scenario.order().setOrderStatus(1);
        scenario.order().setPrintCount(0);
        processOrderMapper.updateById(scenario.order());

        var result = processOrderService.print(scenario.order().getUuid(), new PrintDTO());

        assertThat(result.getFinishRollNos()).isEmpty();
        assertThat(result.getSpareRollNos()).isEmpty();
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(2);
    }

    @Test
    void onSiteSawBackRecord_withActualKnifeCount_recalculatesFeeAndCompletesOrder() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.getSteps().getFirst().setKnifeCount(7);

        processOrderService.backRecord(scenario.order().getUuid(), request);

        ProcessStep step = processStepMapper.selectById(scenario.step().getUuid());
        ProcessOrder order = processOrderMapper.selectById(scenario.order().getUuid());
        assertThat(step.getKnifeCount()).isEqualTo(7);
        assertThat(step.getStepAmount()).isEqualByComparingTo("84");
        assertThat(order.getActualTotalKnife()).isEqualTo(7);
        assertThat(order.getTotalProcessAmount()).isEqualByComparingTo("84.00");
        assertThat(order.getOrderStatus()).isEqualTo(4);
    }

    @Test
    void onSiteSawBackRecord_withoutActualKnifeCount_rejectsSubmission() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.getSteps().getFirst().setKnifeCount(null);

        assertThatThrownBy(() -> processOrderService.backRecord(scenario.order().getUuid(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("现场定尺锯纸必须填写实际刀数");
    }

    @Test
    void onSiteBackRecord_withRetainedTrim_createsRemainInventoryAndSnapshot() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.getFinishes().stream()
                .filter(finish -> finish.getActualWeight() != null)
                .forEach(finish -> finish.setActualWeight(new BigDecimal("90.000")));
        request.setTrims(List.of(fixture.trim(scenario.roll(), 120, "20.000")));

        processOrderService.backRecord(scenario.order().getUuid(), request);

        FinishRoll trim = finishRollMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, scenario.order().getUuid())
                        .eq(FinishRoll::getIsRemain, 1)).get(0);
        assertThat(trim.getFinishWidth()).isEqualTo(120);
        assertThat(trim.getActualWeight()).isEqualByComparingTo("20.000");
        assertThat(trim.getRemainingWeight()).isEqualByComparingTo("20.000");
        assertThat(trim.getFinishStatus()).isEqualTo(2);
        assertThat(trim.getRollNoStatus()).isEqualTo(2);
        assertThat(relationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getFinishUuid, trim.getUuid())))
                .extracting(FinishOriginalRel::getOriginalUuid)
                .containsExactly(scenario.roll().getUuid());
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getSnapFinish())
                .contains("\"finish_width\": 120")
                .contains("\"is_remain\": 1");
    }

    private List<FinishRoll> finishes(ProcessOrder order) {
        return finishRollMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, order.getUuid()));
    }


}

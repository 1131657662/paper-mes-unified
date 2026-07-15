package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.dto.BackRecordStepDTO;
import com.paper.mes.processorder.dto.BackRecordTrimDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BackRecordOnSiteFixture {

    private final BusinessFlowFixtureFactory fixtures;
    private final CustomerMapper customerMapper;
    private final ProcessOrderMapper processOrderMapper;
    private final OriginalRollMapper originalRollMapper;
    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper relationMapper;
    private final ProcessStepMapper processStepMapper;
    private final JdbcTemplate jdbcTemplate;

    Scenario arrange() {
        BusinessFlowFixtureFactory.Scenario base = fixtures.createCompletedOrderWithTwoFinishes();
        setSawPrice(base.customer());
        OriginalRoll roll = originalRoll(base.order());
        originalRollMapper.insert(roll);
        ProcessStep step = prepareStep(base.order(), roll);
        prepareFinish(base.first(), roll);
        prepareFinish(base.second(), roll);
        FinishRoll spare = spare(base.order());
        finishRollMapper.insert(spare);
        base.order().setOrderStatus(3);
        base.order().setSnapFinish(null);
        processOrderMapper.updateById(base.order());
        return new Scenario(base.order(), roll, step, base.first(), base.second(), spare);
    }

    BackRecordDTO request(Scenario scenario, Integer firstWidth, Integer secondWidth) {
        BackRecordDTO dto = new BackRecordDTO();
        dto.setRolls(List.of(rollDto(scenario.roll())));
        dto.setFinishes(List.of(finishDto(scenario.first(), firstWidth),
                finishDto(scenario.second(), secondWidth), finishDto(scenario.spare(), null)));
        BackRecordStepDTO step = new BackRecordStepDTO();
        step.setUuid(scenario.step().getUuid());
        step.setLossWeight(BigDecimal.ZERO);
        step.setKnifeCount(2);
        dto.setSteps(List.of(step));
        return dto;
    }

    BackRecordTrimDTO trim(OriginalRoll source, int width, String weight) {
        BackRecordTrimDTO dto = new BackRecordTrimDTO();
        dto.setOriginalUuid(source.getUuid());
        dto.setFinishWidth(width);
        dto.setActualWeight(new BigDecimal(weight));
        dto.setActualRemark("保留余料");
        return dto;
    }

    private void setSawPrice(Customer customer) {
        customer.setSawPrice(new BigDecimal("12.00"));
        customerMapper.updateById(customer);
    }

    private ProcessStep prepareStep(ProcessOrder order, OriginalRoll roll) {
        ProcessStep step = processStepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, order.getUuid())).getFirst();
        step.setOriginalUuid(roll.getUuid());
        step.setUnitPrice(new BigDecimal("12.00"));
        processStepMapper.updateById(step);
        return step;
    }

    private void prepareFinish(FinishRoll finish, OriginalRoll roll) {
        jdbcTemplate.update("""
                UPDATE biz_finish_roll
                SET finish_width = 0, actual_weight = NULL, remaining_weight = NULL,
                    finish_status = 1, roll_no_status = 1
                WHERE uuid = ?
                """, finish.getUuid());
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOrderUuid(finish.getOrderUuid());
        relation.setFinishUuid(finish.getUuid());
        relation.setOriginalUuid(roll.getUuid());
        relation.setShareRatio(new BigDecimal("100.00"));
        relationMapper.insert(relation);
    }

    private OriginalRoll originalRoll(ProcessOrder order) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(id());
        roll.setOrderUuid(order.getUuid());
        roll.setRowSort(1);
        roll.setRollNo("IT-ONSITE");
        roll.setPaperName("integration-paper");
        roll.setGramWeight(80);
        roll.setOriginalWidth(1200);
        roll.setRollWeight(new BigDecimal("200.000"));
        roll.setPieceNum(1);
        roll.setTotalWeight(new BigDecimal("200.000"));
        roll.setProcessMode(2);
        roll.setMainStepType(1);
        roll.setRollStatus(3);
        return roll;
    }

    private FinishRoll spare(ProcessOrder order) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(id());
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(3);
        finish.setFinishRollNo("Z-SPARE");
        finish.setRollNoStatus(1);
        finish.setIsSpare(1);
        finish.setPaperName("integration-paper");
        finish.setGramWeight(80);
        finish.setFinishWidth(0);
        finish.setSourceType(1);
        finish.setFinishStatus(1);
        return finish;
    }

    private BackRecordRollDTO rollDto(OriginalRoll roll) {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid(roll.getUuid());
        dto.setActualGramWeight(80);
        dto.setActualWidth(1200);
        dto.setActualWeight(new BigDecimal("200.000"));
        return dto;
    }

    private BackRecordFinishDTO finishDto(FinishRoll finish, Integer width) {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setUuid(finish.getUuid());
        dto.setFinishWidth(width);
        dto.setActualWeight(finish.getIsSpare() == 1 ? null : new BigDecimal("100.000"));
        dto.setIsRemain(0);
        dto.setIsAbnormal(0);
        return dto;
    }

    private static String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    record Scenario(ProcessOrder order, OriginalRoll roll, ProcessStep step,
                    FinishRoll first, FinishRoll second, FinishRoll spare) {
    }
}

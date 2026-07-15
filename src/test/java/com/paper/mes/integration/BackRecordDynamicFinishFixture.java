package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
class BackRecordDynamicFinishFixture {

    private final BackRecordOnSiteFixture onSiteFixture;
    private final FinishOriginalRelMapper relationMapper;
    private final FinishRollMapper finishRollMapper;

    BackRecordOnSiteFixture.Scenario arrangeWithoutFinishes() {
        BackRecordOnSiteFixture.Scenario scenario = onSiteFixture.arrange();
        relationMapper.delete(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOrderUuid, scenario.order().getUuid()));
        finishRollMapper.delete(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, scenario.order().getUuid()));
        return scenario;
    }

    BackRecordDTO request(BackRecordOnSiteFixture.Scenario scenario) {
        BackRecordDTO dto = onSiteFixture.request(scenario, null, null);
        dto.setFinishes(List.of(dynamicFinish(scenario)));
        dto.getSteps().getFirst().setLossWeight(new BigDecimal("20.000"));
        return dto;
    }

    private BackRecordFinishDTO dynamicFinish(BackRecordOnSiteFixture.Scenario scenario) {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setOriginalUuid(scenario.roll().getUuid());
        dto.setFinishWidth(900);
        dto.setFinishDiameter(40);
        dto.setFinishCoreDiameter(3);
        dto.setActualWeight(new BigDecimal("180.000"));
        dto.setIsRemain(0);
        dto.setIsAbnormal(0);
        return dto;
    }
}

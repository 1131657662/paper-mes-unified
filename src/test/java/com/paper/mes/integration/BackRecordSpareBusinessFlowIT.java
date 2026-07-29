package com.paper.mes.integration;

import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class BackRecordSpareBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private FinishRollMapper finishRollMapper;

    @Test
    void backRecord_usedSpare_becomesFormalAddedInventory() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.getFinishes().stream()
                .filter(row -> !row.getUuid().equals(scenario.spare().getUuid()))
                .forEach(row -> row.setActualWeight(new BigDecimal("90.000")));
        BackRecordFinishDTO spare = request.getFinishes().stream()
                .filter(row -> row.getUuid().equals(scenario.spare().getUuid()))
                .findFirst().orElseThrow();
        spare.setOriginalUuid(scenario.roll().getUuid());
        spare.setFinishWidth(300);
        spare.setActualWeight(new BigDecimal("20.000"));

        processOrderService.backRecord(scenario.order().getUuid(), request);

        FinishRoll stored = finishRollMapper.selectById(scenario.spare().getUuid());
        assertThat(stored.getIsSpare()).isZero();
        assertThat(stored.getProductionResult()).isEqualTo(4);
        assertThat(stored.getProductionAdjustmentReason()).isEqualTo("备用卷号实际启用");
        assertThat(stored.getFinishStatus()).isEqualTo(2);
        assertThat(stored.getRollNoStatus()).isEqualTo(2);
        assertThat(stored.getRemainingWeight()).isEqualByComparingTo("20.000");
    }

    @Test
    void backRecord_unusedSpare_isVoidedAndReported() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.setFinishes(request.getFinishes().stream()
                .filter(row -> !row.getUuid().equals(scenario.spare().getUuid()))
                .toList());

        BackRecordResultVO result = processOrderService.backRecord(scenario.order().getUuid(), request);

        FinishRoll stored = finishRollMapper.selectById(scenario.spare().getUuid());
        assertThat(stored.getIsSpare()).isOne();
        assertThat(stored.getRollNoStatus()).isEqualTo(3);
        assertThat(result.getVoidedSpareCount()).isOne();
    }
}

package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettleOrderFinishTotalsTest {

    @Test
    void apply_sharedFinishAcrossMultipleSources_countsItOnce() {
        List<SettlePrintLineVO> lines = List.of(new SettlePrintLineVO(), new SettlePrintLineVO(), new SettlePrintLineVO());
        FinishRoll shared = finish("finish-1", "3");

        SettleOrderFinishTotals.apply(lines, List.of(shared, shared, shared));

        assertThat(lines).allSatisfy(line -> {
            assertThat(line.getOrderFinishCount()).isEqualTo(1);
            assertThat(line.getOrderFinishWeight()).isEqualByComparingTo("3");
        });
    }

    @Test
    void apply_excludesSpareRemainAndVoidedFinishes() {
        FinishRoll spare = finish("spare", "10");
        spare.setIsSpare(1);
        FinishRoll remain = finish("remain", "20");
        remain.setIsRemain(1);
        FinishRoll voided = finish("voided", "30");
        voided.setRollNoStatus(3);
        SettlePrintLineVO line = new SettlePrintLineVO();

        SettleOrderFinishTotals.apply(List.of(line), List.of(finish("formal", "4"), spare, remain, voided));

        assertThat(line.getOrderFinishCount()).isEqualTo(1);
        assertThat(line.getOrderFinishWeight()).isEqualByComparingTo("4");
    }

    private FinishRoll finish(String uuid, String weight) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setActualWeight(new BigDecimal(weight));
        return finish;
    }
}

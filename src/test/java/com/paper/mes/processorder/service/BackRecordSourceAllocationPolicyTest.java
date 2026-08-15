package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WeightStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackRecordSourceAllocationPolicyTest {

    @Test
    void recalculate_withThreeMeasuredSources_usesMeasuredContributionShares() {
        List<FinishOriginalRel> relations = relations("100", "100", "100");

        List<FinishOriginalRel> changed = BackRecordSourceAllocationPolicy.recalculate(
                rolls("600", "700", "700"), List.of(finish("2000")), relations);

        assertThat(changed).hasSize(3);
        assertThat(relations).extracting(FinishOriginalRel::getShareRatio)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("35.00"), new BigDecimal("35.00"));
        assertThat(relations).extracting(FinishOriginalRel::getShareWeight)
                .containsExactly(new BigDecimal("600.000"), new BigDecimal("700.000"), new BigDecimal("700.000"));
    }

    @Test
    void recalculate_withPartialConsumption_usesConsumedWeightInsteadOfWholeRollWeight() {
        List<FinishOriginalRel> relations = relations("50", "100");

        BackRecordSourceAllocationPolicy.recalculate(
                rolls("600", "700"), List.of(finish("1000")), relations);

        assertThat(relations).extracting(FinishOriginalRel::getShareRatio)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("70.00"));
    }

    @Test
    void recalculate_whenAnySourceIsNotMeasured_keepsAllocationPending() {
        List<OriginalRoll> rolls = rolls("600", null, "700");
        List<FinishOriginalRel> relations = relations("100", "100", "100");

        List<FinishOriginalRel> changed = BackRecordSourceAllocationPolicy.recalculate(
                rolls, List.of(finish("2000")), relations);

        assertThat(changed).isEmpty();
        assertThat(relations).extracting(FinishOriginalRel::getShareRatio).containsOnlyNulls();
    }

    private List<OriginalRoll> rolls(String... weights) {
        return java.util.stream.IntStream.range(0, weights.length)
                .mapToObj(index -> roll("roll-" + (index + 1), weights[index]))
                .toList();
    }

    private OriginalRoll roll(String uuid, String weight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        if (weight != null) {
            roll.setActualWeight(new BigDecimal(weight));
            roll.setWeightStatus(WeightStatus.MEASURED.name());
        }
        return roll;
    }

    private FinishRoll finish(String weight) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setActualWeight(new BigDecimal(weight));
        return finish;
    }

    private List<FinishOriginalRel> relations(String... consumptionRatios) {
        return java.util.stream.IntStream.range(0, consumptionRatios.length)
                .mapToObj(index -> relation("roll-" + (index + 1), consumptionRatios[index]))
                .toList();
    }

    private FinishOriginalRel relation(String rollUuid, String consumptionRatio) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid("finish-1");
        relation.setOriginalUuid(rollUuid);
        relation.setConsumeRatio(new BigDecimal(consumptionRatio));
        return relation;
    }
}

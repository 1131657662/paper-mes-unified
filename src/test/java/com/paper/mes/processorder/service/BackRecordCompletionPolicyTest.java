package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackRecordCompletionPolicyTest {

    @Test
    void shouldVoid_allRollsDisposedAndNoOutput_returnsTrue() {
        assertThat(BackRecordCompletionPolicy.shouldVoid(List.of(), List.of())).isTrue();
    }

    @Test
    void shouldVoid_directShipOutputExists_returnsFalse() {
        FinishRoll finish = new FinishRoll();
        finish.setSourceType(2);
        finish.setActualWeight(new BigDecimal("100.000"));

        assertThat(BackRecordCompletionPolicy.shouldVoid(List.of(), List.of(finish))).isFalse();
    }

    @Test
    void shouldVoid_activeProductionRollWithoutOutput_returnsFalse() {
        assertThat(BackRecordCompletionPolicy.shouldVoid(
                List.of(new OriginalRoll()), List.of())).isFalse();
    }
}

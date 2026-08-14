package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.model.WeightStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OriginalRollWeightPolicyTest {

    @Test
    void missingStatusWithoutWeight_isUnknown() {
        assertThat(OriginalRollWeightPolicy.normalizeEntryStatus((WeightStatus) null, null))
                .isEqualTo("UNKNOWN");
    }

    @Test
    void positiveWeightWithoutStatus_isEstimated() {
        assertThat(OriginalRollWeightPolicy.normalizeEntryStatus((WeightStatus) null, new BigDecimal("1")))
                .isEqualTo("ESTIMATED");
    }

    @Test
    void unknownStatus_discardsPlaceholderWeightSemantics() {
        assertThat(OriginalRollWeightPolicy.normalizeEntryStatus(WeightStatus.UNKNOWN, new BigDecimal("1")))
                .isEqualTo("UNKNOWN");
    }

    @Test
    void estimatedStatus_requiresPositiveWeight() {
        assertThatThrownBy(() -> OriginalRollWeightPolicy.normalizeEntryStatus(
                WeightStatus.ESTIMATED, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标称/估算重量");
    }

    @Test
    void measuredStatus_isReservedForBackRecord() {
        assertThatThrownBy(() -> OriginalRollWeightPolicy.normalizeEntryStatus(
                WeightStatus.MEASURED, new BigDecimal("2000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MEASURED");
    }

    @Test
    void appendStatus_isCaseInsensitiveButRejectsUnknownValues() {
        assertThat(OriginalRollWeightPolicy.normalizeEntryStatus("estimated", new BigDecimal("2")))
                .isEqualTo("ESTIMATED");
        assertThatThrownBy(() -> OriginalRollWeightPolicy.normalizeEntryStatus("BAD", new BigDecimal("2")))
                .isInstanceOf(BusinessException.class);
    }
}

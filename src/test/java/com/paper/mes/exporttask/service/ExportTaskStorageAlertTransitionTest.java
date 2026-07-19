package com.paper.mes.exporttask.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskStorageAlertTransitionTest {
    @Test
    void firstHealthyObservation_establishesBaselineWithoutNotification() {
        var decision = ExportTaskStorageAlertTransition.evaluate("UNKNOWN", "READY", 0);

        assertThat(decision.changed()).isTrue();
        assertThat(decision.notificationRequired()).isFalse();
        assertThat(decision.transitionNo()).isEqualTo(1);
    }

    @Test
    void repeatedUnhealthyObservation_doesNotNotifyAgain() {
        var decision = ExportTaskStorageAlertTransition.evaluate("LOW_SPACE", "LOW_SPACE", 2);

        assertThat(decision.changed()).isFalse();
        assertThat(decision.notificationRequired()).isFalse();
        assertThat(decision.transitionNo()).isEqualTo(2);
    }

    @Test
    void firstUnhealthyObservation_notifiesImmediately() {
        var decision = ExportTaskStorageAlertTransition.evaluate("UNKNOWN", "LOW_SPACE", 0);

        assertThat(decision.notificationRequired()).isTrue();
        assertThat(decision.transitionNo()).isEqualTo(1);
    }

    @Test
    void unhealthyStateChange_notifiesWithNextTransition() {
        var decision = ExportTaskStorageAlertTransition.evaluate("LOW_SPACE", "READ_ONLY", 3);

        assertThat(decision.notificationRequired()).isTrue();
        assertThat(decision.transitionNo()).isEqualTo(4);
    }

    @Test
    void recovery_notifiesOnce() {
        var decision = ExportTaskStorageAlertTransition.evaluate("UNAVAILABLE", "READY", 5);

        assertThat(decision.notificationRequired()).isTrue();
        assertThat(decision.transitionNo()).isEqualTo(6);
    }
}

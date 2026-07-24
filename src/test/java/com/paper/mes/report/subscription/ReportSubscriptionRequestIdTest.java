package com.paper.mes.report.subscription;

import com.paper.mes.report.subscription.service.ReportSubscriptionRequestId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ReportSubscriptionRequestIdTest {

    @Test
    void generate_sameSlotAndRecipient_isDeterministic() {
        LocalDateTime slot = LocalDateTime.of(2026, 7, 20, 8, 0);

        String first = ReportSubscriptionRequestId.generate("subscription", slot, "recipient");
        String second = ReportSubscriptionRequestId.generate("subscription", slot, "recipient");

        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void generate_differentRecipient_changesRequestId() {
        LocalDateTime slot = LocalDateTime.of(2026, 7, 20, 8, 0);

        String first = ReportSubscriptionRequestId.generate("subscription", slot, "recipient-a");
        String second = ReportSubscriptionRequestId.generate("subscription", slot, "recipient-b");

        assertNotEquals(first, second);
    }
}

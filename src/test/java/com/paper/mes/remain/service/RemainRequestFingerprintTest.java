package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainRegistrationCreateDTO;
import com.paper.mes.remain.dto.RemainRegistrationLineDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RemainRequestFingerprintTest {

    @Test
    void registrationFingerprint_isIndependentOfLineOrder() {
        RemainRegistrationCreateDTO first = request(List.of(line("roll-b", "20"), line("roll-a", "10")));
        RemainRegistrationCreateDTO second = request(List.of(line("roll-a", "10"), line("roll-b", "20")));

        assertEquals(RemainRequestFingerprint.registration(first), RemainRequestFingerprint.registration(second));
    }

    @Test
    void registrationFingerprint_changesWhenWeightChanges() {
        RemainRegistrationCreateDTO first = request(List.of(line("roll-a", "10")));
        RemainRegistrationCreateDTO second = request(List.of(line("roll-a", "11")));

        assertNotEquals(RemainRequestFingerprint.registration(first), RemainRequestFingerprint.registration(second));
    }

    @Test
    void financialFingerprints_haveFixedDatabaseSafeLength() {
        String application = RemainRequestFingerprint.application("registration-uuid", "settle-uuid",
                new BigDecimal("1000"), new BigDecimal("100.000"));
        String adjustment = RemainRequestFingerprint.adjustment("registration-uuid", "settle-uuid",
                new BigDecimal("400"), new BigDecimal("40.000"));

        assertEquals(64, application.length());
        assertEquals(64, adjustment.length());
        assertNotEquals(application, adjustment);
    }

    private static RemainRegistrationCreateDTO request(List<RemainRegistrationLineDTO> lines) {
        RemainRegistrationCreateDTO request = new RemainRegistrationCreateDTO();
        request.setOrderUuid("order-1");
        request.setConfirmationName("客户");
        request.setConfirmationChannel("PHONE");
        request.setConfirmationAt(LocalDateTime.parse("2026-08-20T10:00:00"));
        request.setConfirmationEvidence("已核验");
        request.setLines(lines);
        return request;
    }

    private static RemainRegistrationLineDTO line(String rollUuid, String weight) {
        RemainRegistrationLineDTO line = new RemainRegistrationLineDTO();
        line.setSourceFinishRollUuid(rollUuid);
        line.setTransferredSystemWeight(new BigDecimal(weight));
        return line;
    }
}

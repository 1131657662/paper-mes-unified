package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessOrderReissueFingerprintTest {

    @Test
    void samePayloadProducesTheSameSha256Digest() {
        String first = ProcessOrderReissueFingerprint.of("order-1", 7, " customer change ");
        String second = ProcessOrderReissueFingerprint.of("order-1", 7, "customer change");

        org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second)
                .hasSize(64);
    }

    @Test
    void differentExpectedVersionProducesDifferentDigest() {
        String first = ProcessOrderReissueFingerprint.of("order-1", 7, "customer change");
        String second = ProcessOrderReissueFingerprint.of("order-1", 8, "customer change");

        org.assertj.core.api.Assertions.assertThat(first).isNotEqualTo(second);
    }

    @Test
    void replayWithDifferentDigestIsRejected() {
        ProcessOrderIssueVersionService service = new ProcessOrderIssueVersionService(null);
        ProcessOrderIssueVersion row = new ProcessOrderIssueVersion();
        row.setPayloadHash(ProcessOrderReissueFingerprint.of("order-1", 7, "customer change"));

        assertThatThrownBy(() -> service.requireSameRequest(
                row, ProcessOrderReissueFingerprint.of("order-1", 8, "customer change")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void replayWithTheSameDigestIsAccepted() {
        ProcessOrderIssueVersionService service = new ProcessOrderIssueVersionService(null);
        ProcessOrderIssueVersion row = new ProcessOrderIssueVersion();
        String payloadHash = ProcessOrderReissueFingerprint.of("order-1", 7, "customer change");
        row.setPayloadHash(payloadHash);

        assertThatCode(() -> service.requireSameRequest(row, payloadHash))
                .doesNotThrowAnyException();
    }
}

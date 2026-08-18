package com.paper.mes.ai.process.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiAuditHasherTest {

    @Test
    void sha256IsDeterministicAndPreservesFieldBoundaries() {
        String first = ProcessAiAuditHasher.sha256("ab", "c");
        String replay = ProcessAiAuditHasher.sha256("ab", "c");
        String differentBoundaries = ProcessAiAuditHasher.sha256("a", "bc");

        assertThat(first).hasSize(64).isEqualTo(replay);
        assertThat(differentBoundaries).isNotEqualTo(first);
    }
}

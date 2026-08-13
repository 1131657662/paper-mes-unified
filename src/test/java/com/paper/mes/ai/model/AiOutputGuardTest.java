package com.paper.mes.ai.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiOutputGuardTest {

    private final AiOutputGuard guard = new AiOutputGuard();

    @Test
    void missingCitationIsRejected() {
        assertThat(guard.accepts(new AiModelResult("请核对状态。", List.of()), List.of("E001")))
                .isFalse();
    }

    @Test
    void writeOperationAdviceIsRejected() {
        assertThat(guard.accepts(new AiModelResult("请直接修改数据库。", List.of("E001")), List.of("E001")))
                .isFalse();
    }

    @Test
    void citedReadOnlyAnswerIsAccepted() {
        assertThat(guard.accepts(new AiModelResult("请核对当前状态。", List.of("E001")), List.of("E001")))
                .isTrue();
    }
}

package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessOrderAppendVersionPolicyTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void requireAppendableStatus_forDraftOrPending_allowsResume(int status) {
        assertThatCode(() -> ProcessOrderAppendVersionPolicy.requireAppendableStatus(status))
                .doesNotThrowAnyException();
    }

    @Test
    void requireCurrentVersion_whenOrderChanged_rejectsCommit() {
        assertThatThrownBy(() -> ProcessOrderAppendVersionPolicy.requireCurrentVersion(9, 8))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("其他页面修改");
    }
}

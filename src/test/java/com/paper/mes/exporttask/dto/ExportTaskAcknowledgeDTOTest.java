package com.paper.mes.exporttask.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskAcknowledgeDTOTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void invalidStatusModuleAndKeywordAreRejected() {
        ExportTaskAcknowledgeDTO filter = new ExportTaskAcknowledgeDTO();
        filter.setTaskStatus(7);
        filter.setModuleCode("settle/orders");
        filter.setKeyword("x".repeat(81));

        assertThat(validator.validate(filter)).hasSize(4);
    }

    @Test
    void futureSnapshotIsRejected() {
        ExportTaskAcknowledgeDTO filter = new ExportTaskAcknowledgeDTO();
        filter.setAsOf(LocalDateTime.now().plusMinutes(1));

        assertThat(validator.validate(filter)).extracting("message")
                .containsExactly("数据截点不能晚于当前时间");
    }
}

package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.dto.ExportTaskHistoryQuery;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskHistoryQueryValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void invalidStatusAndModuleAreRejected() {
        ExportTaskHistoryQuery query = new ExportTaskHistoryQuery();
        query.setTaskStatus(7);
        query.setModuleCode("settle/orders");

        assertThat(validator.validate(query)).hasSize(2);
    }

    @Test
    void keywordLongerThanLimitIsRejected() {
        ExportTaskHistoryQuery query = new ExportTaskHistoryQuery();
        query.setKeyword("x".repeat(81));

        assertThat(validator.validate(query)).hasSize(1);
    }
}

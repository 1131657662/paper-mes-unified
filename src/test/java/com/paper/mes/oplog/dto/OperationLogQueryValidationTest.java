package com.paper.mes.oplog.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogQueryValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_withOversizedPage_rejectsQuery() {
        OperationLogQuery query = new OperationLogQuery();
        query.setSize(101);

        assertFalse(validator.validate(query).isEmpty());
    }

    @Test
    void validate_withReversedDateRange_rejectsQuery() {
        OperationLogQuery query = new OperationLogQuery();
        query.setDateFrom(LocalDate.of(2026, 7, 14));
        query.setDateTo(LocalDate.of(2026, 7, 13));

        assertFalse(validator.validate(query).isEmpty());
    }

    @Test
    void validate_withNormalFilters_acceptsQuery() {
        OperationLogQuery query = new OperationLogQuery();
        query.setFieldName("订单状态");
        query.setRemark("回退");

        assertTrue(validator.validate(query).isEmpty());
    }
}

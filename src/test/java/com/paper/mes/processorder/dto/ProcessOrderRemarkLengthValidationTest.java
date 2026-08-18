package com.paper.mes.processorder.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderRemarkLengthValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void draftRejectsShortRemarkLongerThanDatabaseColumn() {
        DraftOrderBaseDTO request = new DraftOrderBaseDTO();
        request.setRemark("x".repeat(256));

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("remark"));
    }

    @Test
    void createRejectsCustomerRequirementLongerThanContract() {
        ProcessOrderCreateDTO request = new ProcessOrderCreateDTO();
        request.setRemarkLong("x".repeat(2001));

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("remarkLong"));
    }
}

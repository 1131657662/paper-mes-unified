package com.paper.mes.report.alert;

import com.paper.mes.report.alert.dto.ReportAlertRuleSaveDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAlertRuleSaveDTOValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenThresholdExceedsPercentRange_rejectsRule() {
        ReportAlertRuleSaveDTO dto = new ReportAlertRuleSaveDTO();
        dto.setSignalCode("LOSS_RATIO");
        dto.setRuleName("损耗率上限校验");
        dto.setScopeType(1);
        dto.setThresholdValue(new BigDecimal("100.01"));
        dto.setSeverity(1);

        assertThat(validator.validate(dto)).extracting("message")
                .contains("阈值不能大于100");
    }
}

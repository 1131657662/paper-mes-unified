package com.paper.mes.report;

import com.paper.mes.report.dto.ReportQuery;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportQueryValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenDateRangeIsReversed_rejectsQuery() {
        ReportQuery query = new ReportQuery();
        query.setDateFrom(LocalDate.of(2026, 7, 19));
        query.setDateTo(LocalDate.of(2026, 7, 1));

        assertThat(validator.validate(query)).extracting("message")
                .contains("开始日期不能晚于结束日期");
    }

    @Test
    void validate_whenDimensionIsUnknown_rejectsQuery() {
        ReportQuery query = new ReportQuery();
        query.setDimension("unknown");

        assertThat(validator.validate(query)).extracting("message")
                .contains("统计维度无效");
    }
}

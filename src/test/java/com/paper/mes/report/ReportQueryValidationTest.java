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

    @Test
    void validate_whenInvoiceTypeIsNoInvoice_acceptsValueTwo() {
        ReportQuery query = new ReportQuery();
        query.setIsInvoice(2);

        assertThat(validator.validate(query)).isEmpty();
    }

    @Test
    void validate_whenMetricReleaseUuidIsMalformed_rejectsQuery() {
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid("not-a-release-id");

        assertThat(validator.validate(query)).isNotEmpty();
    }

    @Test
    void validate_whenInvoiceTypeIsOutsideDictionary_rejectsValue() {
        ReportQuery query = new ReportQuery();
        query.setIsInvoice(3);

        assertThat(validator.validate(query)).extracting("message")
                .contains("开票状态无效");
    }

    @Test
    void validate_whenProcessStepTypeIsOutsideCatalog_rejectsValue() {
        ReportQuery query = new ReportQuery();
        query.setProcessStepType(5);

        assertThat(validator.validate(query)).extracting("message")
                .contains("工序类型无效");
    }
}

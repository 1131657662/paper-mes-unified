package com.paper.mes.report.savedview;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.savedview.dto.ReportSavedViewSaveDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSavedViewSaveDTOValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_unknownReportPath_isRejected() {
        ReportSavedViewSaveDTO dto = validDto();
        dto.setReportPath("/users");

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void validate_emptyMetricSelection_isRejected() {
        ReportSavedViewSaveDTO dto = validDto();
        dto.setMetricCodes(List.of());

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void validate_explorerView_isAccepted() {
        assertThat(validator.validate(validDto())).isEmpty();
    }

    private ReportSavedViewSaveDTO validDto() {
        ReportSavedViewSaveDTO dto = new ReportSavedViewSaveDTO();
        dto.setViewName("重点客户损耗");
        dto.setReportPath("/reports/explorer");
        dto.setReportQuery(new ReportQuery());
        dto.setDimensionCode("customer");
        dto.setMetricCodes(List.of("loss_ratio_pct"));
        dto.setIsDefault(0);
        return dto;
    }
}

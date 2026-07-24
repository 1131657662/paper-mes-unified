package com.paper.mes.report.subscription;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.subscription.dto.ReportSubscriptionSaveDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSubscriptionSaveDTOValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_legacyUserIdentifier_isAccepted() {
        ReportSubscriptionSaveDTO dto = validDto();
        dto.setRecipientUuids(Set.of("u-operator"));

        var violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_pinnedReleaseWithoutUuid_isRejected() {
        ReportSubscriptionSaveDTO dto = validDto();
        dto.setReleasePolicy(2);

        var violations = validator.validate(dto);

        assertThat(violations).anyMatch(item -> item.getMessage().contains("固定发布包"));
    }

    @Test
    void validate_pinnedReleaseWithUuid_isAccepted() {
        ReportSubscriptionSaveDTO dto = validDto();
        dto.setReleasePolicy(2);
        dto.setPinnedReleaseUuid("release-1");

        var violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    private ReportSubscriptionSaveDTO validDto() {
        ReportSubscriptionSaveDTO dto = new ReportSubscriptionSaveDTO();
        dto.setSubscriptionName("昨日加工日报");
        dto.setScheduleType(1);
        dto.setExecutionTime(LocalTime.of(8, 0));
        dto.setTimezone("Asia/Shanghai");
        dto.setReportQuery(new ReportQuery());
        dto.setPeriodPolicy(1);
        dto.setIsEnabled(1);
        return dto;
    }
}

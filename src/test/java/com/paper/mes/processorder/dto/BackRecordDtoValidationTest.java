package com.paper.mes.processorder.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackRecordDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void backRecord_emptyRolls_isRejected() {
        BackRecordDTO dto = new BackRecordDTO();
        dto.setExpectedVersion(1);
        dto.setCompleteOrder(true);
        dto.setWarehouseUuid("warehouse-1");
        dto.setRolls(List.of());

        assertInvalid(dto, "rolls");
    }

    @Test
    void completeBackRecord_onlyRequiresVersion() {
        BackRecordCompleteDTO dto = new BackRecordCompleteDTO();
        dto.setExpectedVersion(1);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void finish_negativeScrapWeight_isRejected() {
        BackRecordFinishDTO dto = validFinish();
        dto.setScrapWeight(new BigDecimal("-0.001"));

        assertInvalid(dto, "scrapWeight");
    }

    @Test
    void finish_nonPositiveActualWeight_isRejected() {
        BackRecordFinishDTO dto = validFinish();
        dto.setActualWeight(BigDecimal.ZERO);

        assertInvalid(dto, "actualWeight");
    }

    @Test
    void finish_invalidBinaryFlags_areRejected() {
        BackRecordFinishDTO dto = validFinish();
        dto.setIsRemain(2);
        dto.setIsAbnormal(-1);

        assertInvalid(dto, "isRemain", "isAbnormal");
    }

    @Test
    void finish_unknownProductionAction_isRejected() {
        BackRecordFinishDTO dto = validFinish();
        dto.setProductionAction("DELETE");

        assertInvalid(dto, "productionAction");
    }

    @Test
    void roll_nonPositiveActualMeasurements_areRejected() {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid("roll-1");
        dto.setActualGramWeight(0);
        dto.setActualWidth(-1);
        dto.setActualWeight(BigDecimal.ZERO);

        assertInvalid(dto, "actualGramWeight", "actualWidth", "actualWeight");
    }

    @Test
    void trim_nonPositiveWidthAndWeight_areRejected() {
        BackRecordTrimDTO dto = new BackRecordTrimDTO();
        dto.setOriginalUuid("roll-1");
        dto.setFinishWidth(0);
        dto.setActualWeight(BigDecimal.ZERO);

        assertInvalid(dto, "finishWidth", "actualWeight");
    }

    private BackRecordFinishDTO validFinish() {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setProductionAction("PRODUCED");
        dto.setFinishWidth(800);
        dto.setActualWeight(new BigDecimal("50.000"));
        dto.setScrapWeight(BigDecimal.ZERO);
        dto.setIsRemain(0);
        dto.setIsAbnormal(0);
        return dto;
    }

    private void assertInvalid(Object dto, String... properties) {
        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(properties);
    }
}

package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackRecordRollSubmissionValidatorTest {

    @Test
    void validate_withEveryOrderRollExactlyOnce_acceptsSubmission() {
        List<OriginalRoll> expected = List.of(roll("roll-1", "M001"), roll("roll-2", "M002"));

        assertThatCode(() -> BackRecordRollSubmissionValidator.validate(
                expected, List.of(dto("roll-2"), dto("roll-1"))))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_withMissingRoll_rejectsOrderCompletion() {
        List<OriginalRoll> expected = List.of(roll("roll-1", "M001"), roll("roll-2", "M002"));

        assertThatThrownBy(() -> BackRecordRollSubmissionValidator.validate(expected, List.of(dto("roll-1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("M002");
    }

    @Test
    void validate_withDuplicateRoll_rejectsSubmission() {
        List<OriginalRoll> expected = List.of(roll("roll-1", "M001"));

        assertThatThrownBy(() -> BackRecordRollSubmissionValidator.validate(
                expected, List.of(dto("roll-1"), dto("roll-1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void validate_withRollFromAnotherOrder_rejectsSubmission() {
        List<OriginalRoll> expected = List.of(roll("roll-1", "M001"));

        assertThatThrownBy(() -> BackRecordRollSubmissionValidator.validate(expected, List.of(dto("roll-9"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于本加工单");
    }

    private OriginalRoll roll(String uuid, String rollNo) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRollNo(rollNo);
        return roll;
    }

    private BackRecordRollDTO dto(String uuid) {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid(uuid);
        return dto;
    }
}

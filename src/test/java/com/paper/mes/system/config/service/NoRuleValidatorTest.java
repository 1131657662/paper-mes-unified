package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.entity.SysNoRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoRuleValidatorTest {

    @Test
    void validate_whenGenericRuleIsValid_doesNotThrow() {
        SysNoRule rule = rule(NoRuleBizType.PROCESS_ORDER, "JG", 1, 4, 1);

        assertDoesNotThrow(() -> NoRuleValidator.validate(rule));
    }

    @Test
    void validate_whenFinishRollRuleIsValid_doesNotThrow() {
        SysNoRule rule = rule(NoRuleBizType.FINISH_ROLL, "A", 2, 6, 0);

        assertDoesNotThrow(() -> NoRuleValidator.validate(rule));
    }

    @ParameterizedTest
    @CsvSource({
            "I,2,6,0",
            "O,2,6,0",
            "L,2,6,0",
            "Z,2,6,0",
            "AA,2,6,0",
            "A,1,6,0",
            "A,2,7,0",
            "A,2,6,1"
    })
    void validate_whenFinishRollRuleBreaksFixedFormat_throwsBusinessException(
            String prefix, int patternType, int serialLength, int resetCycle) {
        SysNoRule rule = rule(NoRuleBizType.FINISH_ROLL, prefix, patternType, serialLength, resetCycle);

        assertThrows(BusinessException.class, () -> NoRuleValidator.validate(rule));
    }

    private static SysNoRule rule(String bizType, String prefix, int patternType, int serialLength, int resetCycle) {
        SysNoRule rule = new SysNoRule();
        rule.setBizType(bizType);
        rule.setPrefix(prefix);
        rule.setPatternType(patternType);
        rule.setDatePattern("yyyyMMdd");
        rule.setSerialLength(serialLength);
        rule.setResetCycle(resetCycle);
        return rule;
    }
}

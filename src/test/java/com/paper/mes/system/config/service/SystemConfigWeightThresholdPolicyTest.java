package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import com.paper.mes.system.config.service.impl.SystemConfigWeightThresholdPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigWeightThresholdPolicyTest {

    private SysConfigItemMapper mapper;
    private SystemConfigWeightThresholdPolicy policy;

    @BeforeEach
    void setUp() {
        mapper = mock(SysConfigItemMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                config("process.weightTolerancePercent", "3"),
                config("process.weightBlockTolerancePercent", "5")));
        policy = new SystemConfigWeightThresholdPolicy(mapper);
    }

    @Test
    void validateEffectivePair_whenWarningExceedsBlock_rejectsChange() {
        assertThrows(BusinessException.class,
                () -> policy.validateEffectivePair("process.weightTolerancePercent", "6", 1));
    }

    @Test
    void validateEffectivePair_whenBlockDropsBelowWarning_rejectsChange() {
        assertThrows(BusinessException.class,
                () -> policy.validateEffectivePair("process.weightBlockTolerancePercent", "2", 1));
    }

    @Test
    void validateEffectivePair_whenBlockEqualsWarning_acceptsChange() {
        assertDoesNotThrow(
                () -> policy.validateEffectivePair("process.weightBlockTolerancePercent", "3", 1));
    }

    @Test
    void validateEffectivePair_whenDisabledBlockFallsBackBelowWarning_rejectsChange() {
        SysConfigItem warning = config("process.weightTolerancePercent", "6");
        SysConfigItem block = config("process.weightBlockTolerancePercent", "100");
        block.setStatus(0);
        when(mapper.selectList(any())).thenReturn(List.of(warning, block));

        assertThrows(BusinessException.class,
                () -> policy.validateEffectivePair("process.weightTolerancePercent", "6", 1));
    }

    @Test
    void validateEffectivePair_whenDisablingBlockBreaksEffectivePair_rejectsChange() {
        when(mapper.selectList(any())).thenReturn(List.of(
                config("process.weightTolerancePercent", "6"),
                config("process.weightBlockTolerancePercent", "8")));

        assertThrows(BusinessException.class,
                () -> policy.validateEffectivePair("process.weightBlockTolerancePercent", "8", 0));
    }

    private SysConfigItem config(String key, String value) {
        SysConfigItem item = new SysConfigItem();
        item.setConfigKey(key);
        item.setConfigValue(value);
        item.setStatus(1);
        return item;
    }
}

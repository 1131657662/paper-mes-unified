package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.dto.NoRuleSaveDTO;
import com.paper.mes.system.config.entity.SysNoRule;
import com.paper.mes.system.config.mapper.SysNoRuleMapper;
import com.paper.mes.system.config.service.impl.NoRuleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoRuleServiceImplTest {

    private SysNoRuleMapper mapper;
    private NoRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysNoRuleMapper.class);
        service = new NoRuleServiceImpl(mock(OperationLogService.class), mock(DocumentNoService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void update_withCoreRuleTypeChange_rejectsSave() {
        when(mapper.selectById("rule-1")).thenReturn(coreRule());
        NoRuleSaveDTO dto = dto();
        dto.setBizType("custom_rule");

        assertThrows(BusinessException.class, () -> service.update("rule-1", dto));

        verify(mapper, never()).updateById(any(SysNoRule.class));
    }

    @Test
    void update_withCoreRuleDisabled_rejectsSave() {
        when(mapper.selectById("rule-1")).thenReturn(coreRule());
        NoRuleSaveDTO dto = dto();
        dto.setStatus(0);

        assertThrows(BusinessException.class, () -> service.update("rule-1", dto));

        verify(mapper, never()).updateById(any(SysNoRule.class));
    }

    private SysNoRule coreRule() {
        SysNoRule rule = new SysNoRule();
        rule.setUuid("rule-1");
        rule.setBizType("process_order");
        rule.setStatus(1);
        return rule;
    }

    private NoRuleSaveDTO dto() {
        NoRuleSaveDTO dto = new NoRuleSaveDTO();
        dto.setBizType("process_order");
        dto.setRuleName("加工单号");
        dto.setPrefix("JG");
        dto.setPatternType(1);
        dto.setDatePattern("yyyyMMdd");
        dto.setSerialLength(4);
        dto.setResetCycle(1);
        dto.setStatus(1);
        return dto;
    }
}

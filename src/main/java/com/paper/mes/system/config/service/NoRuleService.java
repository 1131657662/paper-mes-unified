package com.paper.mes.system.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.system.config.dto.NoRulePreviewVO;
import com.paper.mes.system.config.dto.NoRuleQuery;
import com.paper.mes.system.config.dto.NoRuleSaveDTO;
import com.paper.mes.system.config.entity.SysNoRule;

import java.time.LocalDate;

public interface NoRuleService extends IService<SysNoRule> {

    PageResult<SysNoRule> page(NoRuleQuery query);

    SysNoRule getByUuid(String uuid);

    SysNoRule activeRule(String bizType);

    String create(NoRuleSaveDTO dto);

    void update(String uuid, NoRuleSaveDTO dto);

    NoRulePreviewVO preview(String bizType, LocalDate bizDate);
}

package com.paper.mes.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.PageResult;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.dto.NoRulePreviewVO;
import com.paper.mes.system.config.dto.NoRuleQuery;
import com.paper.mes.system.config.dto.NoRuleSaveDTO;
import com.paper.mes.system.config.entity.SysNoRule;
import com.paper.mes.system.config.mapper.SysNoRuleMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import com.paper.mes.system.config.service.NoRuleService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class NoRuleServiceImpl extends ServiceImpl<SysNoRuleMapper, SysNoRule> implements NoRuleService {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Set<String> DATE_PATTERNS = Set.of("yyyyMMdd", "yyyyMM", "yyyy");
    private static final int STATUS_ENABLED = 1;

    private final OperationLogService operationLogService;
    private final DocumentNoService documentNoService;

    public NoRuleServiceImpl(OperationLogService operationLogService,
                             @Lazy DocumentNoService documentNoService) {
        this.operationLogService = operationLogService;
        this.documentNoService = documentNoService;
    }

    @Override
    public PageResult<SysNoRule> page(NoRuleQuery query) {
        Page<SysNoRule> page = page(Page.of(query.getCurrent(), query.getSize()), buildWrapper(query));
        return PageResult.of(page);
    }

    @Override
    public SysNoRule getByUuid(String uuid) {
        SysNoRule rule = getById(uuid);
        if (rule == null) {
            throw new BusinessException("单号规则不存在");
        }
        return rule;
    }

    @Override
    public SysNoRule activeRule(String bizType) {
        SysNoRule rule = getOne(new LambdaQueryWrapper<SysNoRule>()
                .eq(SysNoRule::getBizType, bizType)
                .eq(SysNoRule::getStatus, STATUS_ENABLED)
                .last("LIMIT 1"));
        if (rule == null) {
            throw new BusinessException("未启用单号规则：" + bizType);
        }
        validateRule(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(NoRuleSaveDTO dto) {
        ensureUnique(dto.getBizType(), null);
        SysNoRule rule = new SysNoRule();
        applyDto(rule, dto);
        save(rule);
        record(rule, "新增单号规则");
        return rule.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, NoRuleSaveDTO dto) {
        SysNoRule rule = getByUuid(uuid);
        ensureUnique(dto.getBizType(), uuid);
        Integer version = rule.getVersion();
        applyDto(rule, dto);
        rule.setUuid(uuid);
        rule.setVersion(version);
        ConcurrencyGuard.requireUpdated(updateById(rule));
        record(rule, "编辑单号规则");
    }

    @Override
    public NoRulePreviewVO preview(String bizType, LocalDate bizDate) {
        SysNoRule rule = activeRule(bizType);
        String sequenceKey = documentNoService.sequenceKey(rule, bizDate);
        long current = documentNoService.currentValue(sequenceKey);
        long next = documentNoService.nextPreviewValue(rule, bizDate);
        NoRulePreviewVO vo = new NoRulePreviewVO();
        vo.setBizType(bizType);
        vo.setSequenceKey(sequenceKey);
        vo.setCurrentValue(current);
        vo.setNextValue(next);
        vo.setExampleNo(documentNoService.preview(rule, bizDate, next));
        return vo;
    }

    private LambdaQueryWrapper<SysNoRule> buildWrapper(NoRuleQuery query) {
        LambdaQueryWrapper<SysNoRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysNoRule::getBizType, kw)
                    .or().like(SysNoRule::getRuleName, kw)
                    .or().like(SysNoRule::getPrefix, kw));
        }
        if (StringUtils.hasText(query.getBizType())) {
            wrapper.eq(SysNoRule::getBizType, query.getBizType().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysNoRule::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysNoRule::getBizType);
        return wrapper;
    }

    private void applyDto(SysNoRule rule, NoRuleSaveDTO dto) {
        rule.setBizType(dto.getBizType().trim());
        rule.setRuleName(dto.getRuleName().trim());
        rule.setPrefix(dto.getPrefix().trim().toUpperCase());
        rule.setPatternType(dto.getPatternType());
        rule.setDatePattern(normalizeDatePattern(dto.getDatePattern()));
        rule.setSerialLength(dto.getSerialLength());
        rule.setResetCycle(dto.getResetCycle());
        rule.setStatus(dto.getStatus());
        rule.setRemark(dto.getRemark());
        validateRule(rule);
    }

    private String normalizeDatePattern(String pattern) {
        return StringUtils.hasText(pattern) ? pattern.trim() : "yyyyMMdd";
    }

    private void validateRule(SysNoRule rule) {
        if (!StringUtils.hasText(rule.getPrefix()) || !PREFIX_PATTERN.matcher(rule.getPrefix()).matches()) {
            throw new BusinessException("单号前缀只能包含字母、数字、下划线或横线");
        }
        if (rule.getPatternType() == null || (rule.getPatternType() != 1 && rule.getPatternType() != 2)) {
            throw new BusinessException("单号格式类型不正确");
        }
        if (!DATE_PATTERNS.contains(normalizeDatePattern(rule.getDatePattern()))) {
            throw new BusinessException("日期格式仅支持 yyyyMMdd、yyyyMM、yyyy");
        }
        if (rule.getSerialLength() == null || rule.getSerialLength() < 3 || rule.getSerialLength() > 10) {
            throw new BusinessException("流水位数必须在 3 到 10 之间");
        }
        if (rule.getResetCycle() == null || rule.getResetCycle() < 0 || rule.getResetCycle() > 3) {
            throw new BusinessException("重置周期不正确");
        }
    }

    private void ensureUnique(String bizType, String excludeUuid) {
        LambdaQueryWrapper<SysNoRule> wrapper = new LambdaQueryWrapper<SysNoRule>()
                .eq(SysNoRule::getBizType, bizType.trim());
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(SysNoRule::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("业务类型已存在单号规则：" + bizType);
        }
    }

    private void record(SysNoRule rule, String action) {
        operationLogService.record(OperationLogService.BIZ_TYPE_SYSTEM_CONFIG,
                rule.getUuid(), rule.getBizType(), action, null,
                action + "：" + rule.getRuleName());
    }
}

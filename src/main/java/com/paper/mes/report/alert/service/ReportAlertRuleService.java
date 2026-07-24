package com.paper.mes.report.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.report.alert.dto.ReportAlertRuleSaveDTO;
import com.paper.mes.report.alert.dto.ReportAlertRuleVO;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.alert.mapper.ReportAlertRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportAlertRuleService {
    private final ReportAlertRuleMapper ruleMapper;
    private final CustomerMapper customerMapper;
    private final PaperMapper paperMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;

    public List<ReportAlertRuleVO> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<ReportAlertRule>()
                        .orderByAsc(ReportAlertRule::getSignalCode, ReportAlertRule::getScopeType)
                        .orderByAsc(ReportAlertRule::getRuleName))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public String create(ReportAlertRuleSaveDTO dto) {
        validateReferences(dto);
        ReportAlertRule rule = new ReportAlertRule();
        apply(rule, dto);
        try {
            ConcurrencyGuard.requireRowUpdated(ruleMapper.insert(rule));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("相同信号和作用范围的规则已存在");
        }
        record(rule, "新增报表告警规则");
        return rule.getUuid();
    }

    @Transactional
    public void update(String uuid, ReportAlertRuleSaveDTO dto) {
        if (dto.getVersion() == null) throw new BusinessException("更新规则必须携带数据版本");
        validateReferences(dto);
        ReportAlertRule rule = requireRule(uuid);
        apply(rule, dto);
        rule.setVersion(dto.getVersion());
        try {
            ConcurrencyGuard.requireRowUpdated(ruleMapper.updateById(rule));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("相同信号和作用范围的规则已存在");
        }
        record(rule, "修改报表告警规则");
    }

    @Transactional
    public void delete(String uuid, int version) {
        ReportAlertRule rule = requireRule(uuid);
        int rows = ruleMapper.update(null, new LambdaUpdateWrapper<ReportAlertRule>()
                .eq(ReportAlertRule::getUuid, uuid)
                .eq(ReportAlertRule::getVersion, version)
                .set(ReportAlertRule::getIsDeleted, 1)
                .setSql("version = version + 1"));
        ConcurrencyGuard.requireRowUpdated(rows);
        record(rule, "删除报表告警规则");
    }

    private void validateReferences(ReportAlertRuleSaveDTO dto) {
        Integer signals = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rpt_alert_signal_definition "
                + "WHERE signal_code = ? AND is_enabled = 1", Integer.class, dto.getSignalCode());
        if (signals == null || signals == 0) throw new BusinessException("告警信号不存在或已停用");
        if (dto.getScopeType() == 2 && customerMapper.selectById(dto.getCustomerUuid()) == null) {
            throw new BusinessException("客户不存在");
        }
        if (dto.getScopeType() == 3 && paperMapper.selectById(dto.getPaperUuid()) == null) {
            throw new BusinessException("纸张不存在");
        }
    }

    private void apply(ReportAlertRule rule, ReportAlertRuleSaveDTO dto) {
        rule.setSignalCode(dto.getSignalCode().trim());
        rule.setRuleName(dto.getRuleName().trim());
        rule.setScopeType(dto.getScopeType());
        rule.setCustomerUuid(trim(dto.getCustomerUuid()));
        rule.setPaperUuid(trim(dto.getPaperUuid()));
        rule.setProcessType(dto.getProcessType());
        rule.setComparisonOperator(dto.getComparisonOperator());
        rule.setThresholdValue(dto.getThresholdValue());
        rule.setSeverity(dto.getSeverity());
        rule.setIsEnabled(dto.getIsEnabled());
    }

    private ReportAlertRule requireRule(String uuid) {
        ReportAlertRule rule = ruleMapper.selectById(uuid);
        if (rule == null) throw new BusinessException("报表告警规则不存在");
        return rule;
    }

    private ReportAlertRuleVO toVO(ReportAlertRule rule) {
        return new ReportAlertRuleVO(rule.getUuid(), rule.getSignalCode(), rule.getRuleName(),
                rule.getScopeType(), rule.getCustomerUuid(), rule.getPaperUuid(), rule.getProcessType(),
                rule.getComparisonOperator(), rule.getThresholdValue(), rule.getSeverity(),
                rule.getIsEnabled(), rule.getVersion(), rule.getUpdateTime());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void record(ReportAlertRule rule, String action) {
        operationLogService.record("报表告警规则", rule.getUuid(), rule.getRuleName(), action, null, action);
    }
}

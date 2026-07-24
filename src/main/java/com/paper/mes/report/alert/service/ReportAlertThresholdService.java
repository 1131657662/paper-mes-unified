package com.paper.mes.report.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.report.alert.dto.ReportThresholdContextVO;
import com.paper.mes.report.alert.dto.ReportThresholdItemVO;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.alert.mapper.ReportAlertRuleMapper;
import com.paper.mes.report.dto.ReportQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportAlertThresholdService {
    public static final String LOSS_RATIO = "LOSS_RATIO";
    public static final String UNRECEIVED_RATIO = "UNRECEIVED_RATIO";
    private static final List<String> SIGNALS = List.of(LOSS_RATIO, UNRECEIVED_RATIO);
    private final ReportAlertRuleMapper ruleMapper;
    private final PaperMapper paperMapper;

    public ReportThresholdContextVO resolve(ReportQuery query) {
        String paperUuid = resolvePaperUuid(query.getPaperName());
        List<ReportAlertRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<ReportAlertRule>()
                .in(ReportAlertRule::getSignalCode, SIGNALS)
                .eq(ReportAlertRule::getIsEnabled, 1));
        List<ReportThresholdItemVO> items = SIGNALS.stream()
                .map(signal -> ReportAlertRuleMatcher.best(rules, signal, query, paperUuid))
                .filter(java.util.Objects::nonNull)
                .map(this::toVO).toList();
        return new ReportThresholdContextVO(LocalDateTime.now(), items);
    }

    private String resolvePaperUuid(String paperName) {
        if (!StringUtils.hasText(paperName)) return null;
        List<Paper> papers = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                .eq(Paper::getPaperName, paperName.trim()).last("LIMIT 2"));
        return papers.size() == 1 ? papers.getFirst().getUuid() : null;
    }

    private ReportThresholdItemVO toVO(ReportAlertRule rule) {
        return new ReportThresholdItemVO(rule.getUuid(), rule.getSignalCode(),
                rule.getComparisonOperator(), rule.getThresholdValue(), rule.getSeverity(),
                rule.getScopeType(), scopeLabel(rule));
    }

    private String scopeLabel(ReportAlertRule rule) {
        return switch (rule.getScopeType()) {
            case 2 -> "客户专属";
            case 3 -> "纸张专属";
            case 4 -> Integer.valueOf(1).equals(rule.getProcessType()) ? "锯纸工艺" : "复卷工艺";
            default -> "全局";
        };
    }
}

package com.paper.mes.report.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.alert.mapper.ReportAlertRuleMapper;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportMetricCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAlertEvaluationService {
    private final ReportAlertRuleMapper ruleMapper;
    private final ReportMetricCatalogService metricCatalogService;
    private final ReportAlertEvaluationDatasetLoader datasetLoader;
    private final ReportAlertEventRecorder eventRecorder;
    private final ReportAlertNotificationPublisher notificationPublisher;

    public void evaluate(LocalDate asOf) {
        ReportAlertEvaluationWindow window = ReportAlertEvaluationWindow.currentMonth(asOf);
        eventRecorder.resolveExpired(window.periodStart());
        eventRecorder.resolveInactiveRules();
        List<ReportAlertRule> rules = enabledRules();
        if (rules.isEmpty()) return;
        String releaseUuid = metricCatalogService.activeContext().releaseUuid();
        eventRecorder.resolveSupersededRelease(releaseUuid);
        eventRecorder.resolveOutdatedScopes(releaseUuid, window.periodStart());
        ReportAlertEvaluationDataset dataset = datasetLoader.load(query(window, releaseUuid), rules);
        EvaluationContext context = new EvaluationContext(releaseUuid, window, dataset);
        rules.forEach(rule -> evaluateRule(rule, context));
    }

    private List<ReportAlertRule> enabledRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<ReportAlertRule>()
                .eq(ReportAlertRule::getIsEnabled, 1)
                .orderByAsc(ReportAlertRule::getUuid));
    }

    private ReportQuery query(ReportAlertEvaluationWindow window, String releaseUuid) {
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid(releaseUuid);
        query.setDateFrom(window.periodStart());
        query.setDateTo(window.asOf());
        return query;
    }

    private void evaluateRule(ReportAlertRule rule, EvaluationContext context) {
        var identity = identity(rule, context);
        ReportAlertMetricSnapshot snapshot = context.dataset().snapshot(rule);
        if (snapshot == null || !snapshot.hasData()) {
            eventRecorder.resolve(identity);
            return;
        }
        RuleEvaluation evaluation = new RuleEvaluation(rule, identity, context);
        ReportAlertSignalEvaluator.value(rule.getSignalCode(), snapshot)
                .ifPresentOrElse(value -> applyResult(evaluation, value),
                        () -> log.warn("Unsupported report alert signal: {}", rule.getSignalCode()));
    }

    private void applyResult(RuleEvaluation evaluation, BigDecimal value) {
        ReportAlertRule rule = evaluation.rule();
        if (!ReportAlertSignalEvaluator.matches(
                rule.getComparisonOperator(), value, rule.getThresholdValue())) {
            eventRecorder.resolve(evaluation.identity());
            return;
        }
        var result = eventRecorder.record(new ReportAlertEventRecorder.AlertEvent(
                evaluation.identity().ruleUuid(), evaluation.identity().releaseUuid(),
                evaluation.identity().periodStart(), evaluation.identity().periodEnd(),
                evaluation.identity().canonicalDimensions(), value,
                rule.getThresholdValue(), rule.getSeverity()));
        if (result.opened()) notificationPublisher.publish(
                new ReportAlertNotificationPublisher.AlertNotification(result.eventUuid(), rule, value,
                        evaluation.context().window(), evaluation.context().dataset().scopeLabel(rule)));
    }

    private ReportAlertEventRecorder.AlertIdentity identity(
            ReportAlertRule rule, EvaluationContext context) {
        return new ReportAlertEventRecorder.AlertIdentity(rule.getUuid(), context.releaseUuid(),
                context.window().periodStart(), context.window().periodEnd(),
                context.dataset().canonicalDimensions(rule));
    }

    private record EvaluationContext(
            String releaseUuid,
            ReportAlertEvaluationWindow window,
            ReportAlertEvaluationDataset dataset
    ) {
    }

    private record RuleEvaluation(
            ReportAlertRule rule,
            ReportAlertEventRecorder.AlertIdentity identity,
            EvaluationContext context
    ) {
    }
}

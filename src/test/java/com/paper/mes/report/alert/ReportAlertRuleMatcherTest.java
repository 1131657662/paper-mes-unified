package com.paper.mes.report.alert;

import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.alert.service.ReportAlertRuleMatcher;
import com.paper.mes.report.dto.ReportQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAlertRuleMatcherTest {
    @Test
    void best_customerAndProcessMatch_prefersCustomerRule() {
        ReportQuery query = new ReportQuery();
        query.setCustomerUuid("customer-1");
        query.setMainStepType(2);

        ReportAlertRule result = ReportAlertRuleMatcher.best(List.of(
                rule("global", 1, null, null), rule("process", 4, null, 2),
                rule("customer", 2, "customer-1", null)), "LOSS_RATIO", query, null);

        assertThat(result.getUuid()).isEqualTo("customer");
    }

    @Test
    void best_withoutScopedFilter_returnsGlobalRule() {
        ReportAlertRule result = ReportAlertRuleMatcher.best(
                List.of(rule("global", 1, null, null), rule("customer", 2, "customer-1", null)),
                "LOSS_RATIO", new ReportQuery(), null);

        assertThat(result.getUuid()).isEqualTo("global");
    }

    private ReportAlertRule rule(String uuid, int scope, String customerUuid, Integer processType) {
        ReportAlertRule rule = new ReportAlertRule();
        rule.setUuid(uuid);
        rule.setSignalCode("LOSS_RATIO");
        rule.setScopeType(scope);
        rule.setCustomerUuid(customerUuid);
        rule.setProcessType(processType);
        return rule;
    }
}

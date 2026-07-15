package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import com.paper.mes.health.dto.DataHealthSummaryVO;
import com.paper.mes.notification.service.SystemNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataHealthService {

    private final SettlementHealthInspector settlementInspector;
    private final DeliveryHealthInspector deliveryInspector;
    private final ProductionHealthInspector productionInspector;
    private final WorkflowHealthInspector workflowInspector;
    private final SystemNotificationService notificationService;

    public DataHealthSummaryVO inspect() {
        List<DataHealthIssueVO> issues = new ArrayList<>();
        issues.addAll(settlementInspector.inspect());
        issues.addAll(deliveryInspector.inspect());
        issues.addAll(productionInspector.inspect());
        issues.addAll(workflowInspector.inspect());
        issues.sort(Comparator.comparing(DataHealthIssueVO::severity)
                .thenComparing(DataHealthIssueVO::businessNo,
                        Comparator.nullsLast(String::compareTo)));
        notificationService.publishDataHealthIssues(issues);
        long critical = issues.stream().filter(issue -> "CRITICAL".equals(issue.severity())).count();
        return new DataHealthSummaryVO(
                LocalDateTime.now(), critical, issues.size() - critical, List.copyOf(issues));
    }
}

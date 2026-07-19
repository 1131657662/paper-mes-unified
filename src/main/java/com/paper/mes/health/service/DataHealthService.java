package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import com.paper.mes.health.dto.DataHealthSummaryVO;
import com.paper.mes.notification.service.SystemNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DataHealthService {

    private final List<DataHealthInspector> inspectors;
    private final SystemNotificationService notificationService;

    public DataHealthSummaryVO inspect() {
        List<DataHealthIssueVO> issues = inspectors.stream()
                .flatMap(this::inspectSafely)
                .sorted(Comparator.comparing(DataHealthIssueVO::severity)
                        .thenComparing(DataHealthIssueVO::businessNo,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
        notificationService.publishDataHealthIssues(issues);
        long critical = issues.stream().filter(issue -> "CRITICAL".equals(issue.severity())).count();
        return new DataHealthSummaryVO(
                LocalDateTime.now(), critical, issues.size() - critical, issues);
    }

    private Stream<DataHealthIssueVO> inspectSafely(DataHealthInspector inspector) {
        return inspector.inspect().stream();
    }
}

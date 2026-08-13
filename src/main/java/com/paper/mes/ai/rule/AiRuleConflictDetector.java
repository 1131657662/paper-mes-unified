package com.paper.mes.ai.rule;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AiRuleConflictDetector {

    public List<AiRuleConflict> detect(List<AiRule> rules) {
        List<AiRuleConflict> conflicts = new ArrayList<>();
        for (int left = 0; left < rules.size(); left++) {
            for (int right = left + 1; right < rules.size(); right++) {
                addConflictIfActive(rules.get(left), rules.get(right), conflicts);
            }
        }
        return List.copyOf(conflicts);
    }

    private void addConflictIfActive(AiRule first, AiRule second, List<AiRuleConflict> conflicts) {
        if (!isActive(first) || !isActive(second)) {
            return;
        }
        if (!scopeOverlaps(first, second) || sameOutcome(first, second)) {
            return;
        }
        if (hasApprovedResolution(first, second)) {
            return;
        }
        conflicts.add(new AiRuleConflict(first.ruleId(), second.ruleId(), "适用范围相交但结论互斥"));
    }

    private boolean isActive(AiRule rule) {
        return rule != null && "ACTIVE".equals(rule.status());
    }

    private boolean sameOutcome(AiRule first, AiRule second) {
        return Objects.equals(first.effect(), second.effect())
                && Objects.equals(first.decision(), second.decision());
    }

    private boolean scopeOverlaps(AiRule first, AiRule second) {
        return matches(first.module(), second.module())
                && matches(first.operation(), second.operation())
                && intersects(first.statuses(), second.statuses())
                && intersects(first.roles(), second.roles())
                && intersects(first.conditions(), second.conditions());
    }

    private boolean hasApprovedResolution(AiRule first, AiRule second) {
        if (blank(first.conflictResolutionId()) || blank(second.conflictResolutionId())) {
            return false;
        }
        return Objects.equals(first.conflictResolutionId(), second.conflictResolutionId())
                && (supersedes(first, second) || supersedes(second, first));
    }

    private boolean supersedes(AiRule candidate, AiRule other) {
        return Objects.equals(candidate.supersedes(), other.ruleId())
                && candidate.priority() > other.priority()
                && candidate.specificity() >= other.specificity();
    }

    private boolean intersects(List<String> first, List<String> second) {
        return first.contains("*") || second.contains("*") || first.stream().anyMatch(second::contains);
    }

    private boolean matches(String first, String second) {
        return "*".equals(first) || "*".equals(second) || Objects.equals(first, second);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

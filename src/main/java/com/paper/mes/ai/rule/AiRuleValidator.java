package com.paper.mes.ai.rule;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Component
public class AiRuleValidator {

    private final AiRuleConflictDetector conflictDetector;
    private final AiRuleChecksum checksum;

    public AiRuleValidator(AiRuleConflictDetector conflictDetector, AiRuleChecksum checksum) {
        this.conflictDetector = conflictDetector;
        this.checksum = checksum;
    }

    public void validate(AiRuleArtifact artifact) {
        if (artifact == null || blank(artifact.artifactVersion()) || artifact.rules() == null
                || artifact.rules().isEmpty()) {
            throw new IllegalArgumentException("AI 规则制品为空或缺少版本");
        }
        validateRules(artifact.rules());
        if (artifact.rules().stream().noneMatch(rule -> "ACTIVE".equals(rule.status()))) {
            throw new IllegalArgumentException("AI 规则制品没有可用的 ACTIVE 规则");
        }
        List<AiRuleConflict> conflicts = conflictDetector.detect(artifact.rules());
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("AI 规则存在未裁决冲突: " + conflicts.getFirst());
        }
    }

    private void validateRules(List<AiRule> rules) {
        Set<String> identities = new HashSet<>();
        for (AiRule rule : rules) {
            requireFields(rule);
            if (!identities.add(rule.ruleId() + "@" + rule.version())) {
                throw new IllegalArgumentException("AI 规则标识重复: " + rule.ruleId());
            }
            if (!Objects.equals(rule.checksum(), checksum.calculate(rule))) {
                throw new IllegalArgumentException("AI 规则 checksum 无效: " + rule.ruleId());
            }
        }
    }

    private void requireFields(AiRule rule) {
        if (rule == null || blank(rule.ruleId()) || blank(rule.version()) || blank(rule.module())
                || blank(rule.operation()) || blank(rule.decision()) || blank(rule.effect())
                || blank(rule.title()) || blank(rule.answer()) || blank(rule.checksum())
                || blank(rule.effectiveDate()) || blank(rule.businessOwner())
                || blank(rule.reviewer()) || blank(rule.source())) {
            throw new IllegalArgumentException("AI 规则缺少必填字段");
        }
        if (empty(rule.statuses()) || empty(rule.roles()) || empty(rule.conditions())
                || empty(rule.keywords()) || empty(rule.safeNextSteps()) || empty(rule.prohibited())) {
            throw new IllegalArgumentException("AI 规则适用范围或关键词为空: " + rule.ruleId());
        }
        if (containsBlank(rule.statuses()) || containsBlank(rule.roles()) || containsBlank(rule.conditions())
                || containsBlank(rule.keywords()) || containsBlank(rule.safeNextSteps())
                || containsBlank(rule.prohibited())) {
            throw new IllegalArgumentException("AI 规则列表字段包含空值: " + rule.ruleId());
        }
        if (!Set.of("ACTIVE", "INACTIVE").contains(rule.status())) {
            throw new IllegalArgumentException("AI 规则 status 非法: " + rule.ruleId());
        }
        if (!Set.of("ANSWER", "CLARIFY", "REFUSE").contains(rule.decision())) {
            throw new IllegalArgumentException("AI 规则 decision 非法: " + rule.ruleId());
        }
    }

    private boolean empty(List<String> values) {
        return values == null || values.isEmpty();
    }

    private boolean containsBlank(List<String> values) {
        return values.stream().anyMatch(this::blank);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

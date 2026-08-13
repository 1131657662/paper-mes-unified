package com.paper.mes.ai.rule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRuleValidatorTest {

    private final AiRuleChecksum checksum = new AiRuleChecksum();
    private final AiRuleValidator validator = new AiRuleValidator(new AiRuleConflictDetector(), checksum);

    @Test
    void emptyArtifactIsRejected() {
        assertThatThrownBy(() -> validator.validate(new AiRuleArtifact("rules-v1", List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("为空");
    }

    @Test
    void duplicateIdentityIsRejected() {
        var rule = signed(AiRuleTestFactory.rule("RULE-A", "allow"));

        assertThatThrownBy(() -> validator.validate(new AiRuleArtifact("rules-v1", List.of(rule, rule))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void tamperedRuleIsRejected() {
        var rule = AiRuleTestFactory.rule("RULE-A", "allow");

        assertThatThrownBy(() -> validator.validate(new AiRuleArtifact("rules-v1", List.of(rule))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checksum");
    }

    @Test
    void missingGovernanceMetadataIsRejected() {
        var rule = AiRuleTestFactory.rule("RULE-A", "allow");
        var incomplete = new AiRule(rule.ruleId(), rule.version(), rule.status(), rule.module(), rule.operation(),
                rule.statuses(), rule.roles(), rule.conditions(), rule.decision(), rule.effect(), rule.title(),
                rule.keywords(), rule.answer(), rule.safeNextSteps(), rule.prohibited(), "", rule.businessOwner(),
                rule.reviewer(), rule.source(), rule.priority(), rule.specificity(), rule.supersedes(),
                rule.conflictResolutionId(), checksum.calculate(new AiRule(rule.ruleId(), rule.version(), rule.status(),
                        rule.module(), rule.operation(), rule.statuses(), rule.roles(), rule.conditions(), rule.decision(),
                        rule.effect(), rule.title(), rule.keywords(), rule.answer(), rule.safeNextSteps(), rule.prohibited(),
                        "", rule.businessOwner(), rule.reviewer(), rule.source(), rule.priority(), rule.specificity(),
                        rule.supersedes(), rule.conflictResolutionId(), "placeholder")));

        assertThatThrownBy(() -> validator.validate(new AiRuleArtifact("rules-v1", List.of(incomplete))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必填");
    }

    @Test
    void artifactWithoutActiveRulesIsRejected() {
        var rule = AiRuleTestFactory.rule("RULE-A", "allow");
        var inactive = withStatus(rule, "INACTIVE");

        assertThatThrownBy(() -> validator.validate(new AiRuleArtifact("rules-v1", List.of(inactive))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void blankListEntryIsRejected() {
        var rule = AiRuleTestFactory.rule("RULE-A", "allow");
        var incomplete = new AiRule(rule.ruleId(), rule.version(), rule.status(), rule.module(), rule.operation(),
                rule.statuses(), rule.roles(), rule.conditions(), rule.decision(), rule.effect(), rule.title(),
                List.of("E001", " "), rule.answer(), rule.safeNextSteps(), rule.prohibited(), rule.effectiveDate(),
                rule.businessOwner(), rule.reviewer(), rule.source(), rule.priority(), rule.specificity(),
                rule.supersedes(), rule.conflictResolutionId(), "placeholder");
        var signed = withChecksum(incomplete);

        assertThatThrownBy(() -> validator.validate(new AiRuleArtifact("rules-v1", List.of(signed))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("空值");
    }

    private AiRule signed(AiRule rule) {
        return AiRuleTestFactory.rule(rule.ruleId(), rule.effect(), rule.conditions(), rule.supersedes(),
                rule.conflictResolutionId(), rule.priority(), rule.specificity(), checksum.calculate(rule));
    }

    private AiRule withStatus(AiRule rule, String status) {
        return withChecksum(new AiRule(rule.ruleId(), rule.version(), status, rule.module(), rule.operation(),
                rule.statuses(), rule.roles(), rule.conditions(), rule.decision(), rule.effect(), rule.title(),
                rule.keywords(), rule.answer(), rule.safeNextSteps(), rule.prohibited(), rule.effectiveDate(),
                rule.businessOwner(), rule.reviewer(), rule.source(), rule.priority(), rule.specificity(),
                rule.supersedes(), rule.conflictResolutionId(), "placeholder"));
    }

    private AiRule withChecksum(AiRule rule) {
        return new AiRule(rule.ruleId(), rule.version(), rule.status(), rule.module(), rule.operation(),
                rule.statuses(), rule.roles(), rule.conditions(), rule.decision(), rule.effect(), rule.title(),
                rule.keywords(), rule.answer(), rule.safeNextSteps(), rule.prohibited(), rule.effectiveDate(),
                rule.businessOwner(), rule.reviewer(), rule.source(), rule.priority(), rule.specificity(),
                rule.supersedes(), rule.conflictResolutionId(), checksum.calculate(rule));
    }
}

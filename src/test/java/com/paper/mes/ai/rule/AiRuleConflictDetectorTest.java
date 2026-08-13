package com.paper.mes.ai.rule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiRuleConflictDetectorTest {

    private final AiRuleConflictDetector detector = new AiRuleConflictDetector();

    @Test
    void overlappingRulesWithDifferentEffectsAreRejected() {
        var conflicts = detector.detect(List.of(
                AiRuleTestFactory.rule("RULE-A", "allow"),
                AiRuleTestFactory.rule("RULE-B", "deny")));

        assertThat(conflicts).singleElement().satisfies(conflict ->
                assertThat(conflict.reason()).contains("结论互斥"));
    }

    @Test
    void rulesWithDifferentConditionsDoNotConflict() {
        var conflicts = detector.detect(List.of(
                AiRuleTestFactory.rule("RULE-A", "allow", List.of("error:E001"), "", "", 10, 10, "x"),
                AiRuleTestFactory.rule("RULE-B", "deny", List.of("error:E002"), "", "", 10, 10, "x")));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void sameEffectWithDifferentDecisionsStillConflicts() {
        var first = AiRuleTestFactory.rule("RULE-A", "same-effect");
        var second = new AiRule("RULE-B", "1.0.0", "ACTIVE", "process-order", "state-transition",
                List.of("ISSUED"), List.of("order_clerk"), List.of("error:E001"), "REFUSE",
                "same-effect", "状态规则", List.of("E001"), "请联系管理员。", List.of("联系管理员"),
                List.of("绕过校验"), "2026-08-13", "生产负责人", "技术负责人",
                "rules/process-order.md", 10, 10, "", "", "placeholder");

        assertThat(detector.detect(List.of(first, second))).hasSize(1);
    }

    @Test
    void explicitSupersessionRequiresTheSameResolutionRecord() {
        var older = AiRuleTestFactory.rule("RULE-A", "allow", List.of("*"), "", "CR-1", 10, 10, "x");
        var newer = AiRuleTestFactory.rule("RULE-B", "deny", List.of("*"), "RULE-A", "CR-2", 20, 10, "x");

        assertThat(detector.detect(List.of(older, newer))).hasSize(1);
    }

    @Test
    void approvedMoreSpecificSupersessionResolvesConflict() {
        var older = AiRuleTestFactory.rule("RULE-A", "allow", List.of("*"), "", "CR-1", 10, 10, "x");
        var newer = AiRuleTestFactory.rule("RULE-B", "deny", List.of("*"), "RULE-A", "CR-1", 20, 10, "x");

        assertThat(detector.detect(List.of(older, newer))).isEmpty();
    }

    @Test
    void inactiveHistoricalRuleDoesNotBlockActiveCatalog() {
        var inactive = new AiRule("RULE-A", "1.0.0", "INACTIVE", "process-order", "state-transition",
                List.of("ISSUED"), List.of("order_clerk"), List.of("error:E001"), "ANSWER",
                "allow", "历史状态规则", List.of("E001"), "旧规则", List.of("联系管理员"),
                List.of("绕过校验"), "2026-08-13", "生产负责人", "技术负责人",
                "rules/process-order.md", 10, 10, "", "", "placeholder");

        assertThat(detector.detect(List.of(inactive, AiRuleTestFactory.rule("RULE-B", "deny")))).isEmpty();
    }
}

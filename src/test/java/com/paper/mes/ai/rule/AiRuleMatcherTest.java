package com.paper.mes.ai.rule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiRuleMatcherTest {

    @Test
    void inactiveRulesAreNeverReturned() {
        AiRule active = rule("ACTIVE-RULE", "ACTIVE");
        AiRule inactive = rule("INACTIVE-RULE", "INACTIVE");

        var matched = new AiRuleMatcher().match("E001", List.of(inactive, active));

        assertThat(matched).extracting(AiRule::ruleId).containsExactly("ACTIVE-RULE");
    }

    @Test
    void globalAssistantStillFindsExplicitRuleFromAnotherPage() {
        AiRule active = rule("ACTIVE-RULE", "ACTIVE");

        var matched = new AiRuleMatcher().match("E001", "dashboard", List.of(active));

        assertThat(matched).extracting(AiRule::ruleId).containsExactly("ACTIVE-RULE");
    }

    private AiRule rule(String id, String status) {
        return new AiRule(id, "1.0.0", status, "process-order", "state-transition",
                List.of("*"), List.of("*"), List.of("error:E001"), "ANSWER", "explain",
                "状态规则", List.of("E001"), "请核对当前状态。", List.of("刷新页面"),
                List.of("绕过校验"), "2026-08-13", "生产负责人", "技术负责人",
                "src/main/java/com/paper/mes/common/ErrorCode.java", 10, 10, "", "", "test");
    }
}

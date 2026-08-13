package com.paper.mes.ai.rule;

import java.util.List;

final class AiRuleTestFactory {

    private AiRuleTestFactory() {
    }

    static AiRule rule(String id, String effect) {
        return rule(id, effect, List.of("error:E001"), "", "", 10, 10, "placeholder");
    }

    static AiRule rule(String id, String effect, List<String> conditions, String supersedes,
                       String resolutionId, int priority, int specificity, String checksum) {
        return new AiRule(id, "1.0.0", "ACTIVE", "process-order", "state-transition",
                List.of("ISSUED"), List.of("order_clerk"), conditions, "ANSWER", effect,
                "状态规则", List.of("E001"), "请核对当前状态。", List.of("刷新页面"),
                List.of("绕过校验"), "2026-08-13", "生产负责人", "技术负责人",
                "rules/process-order.md", priority, specificity, supersedes, resolutionId, checksum);
    }
}

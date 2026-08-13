package com.paper.mes.ai.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AiOutputGuard {

    private static final Set<String> UNSAFE_MARKERS = Set.of(
            "drop table", "delete from", "update ", "insert into", "sudo ", "rm -rf",
            "shell", "password", "token", "cookie", "api key", "api_key", "jwt",
            "密码", "令牌", "服务器命令", "直接修改数据库", "绕过审批", "绕过权限");

    public boolean accepts(AiModelResult result, List<String> allowedRuleIds) {
        if (result == null || blank(result.answer()) || result.answer().length() > 4_000) {
            return false;
        }
        if (result.citationRuleIds() == null || result.citationRuleIds().isEmpty()
                || result.citationRuleIds().stream().anyMatch(id -> !allowedRuleIds.contains(id))) {
            return false;
        }
        String lower = result.answer().toLowerCase(Locale.ROOT);
        return UNSAFE_MARKERS.stream().noneMatch(lower::contains);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

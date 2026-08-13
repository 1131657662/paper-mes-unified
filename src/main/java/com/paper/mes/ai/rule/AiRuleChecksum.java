package com.paper.mes.ai.rule;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class AiRuleChecksum {

    public String calculate(AiRule rule) {
        StringBuilder payload = new StringBuilder();
        append(payload, "ruleId", rule.ruleId());
        append(payload, "version", rule.version());
        append(payload, "status", rule.status());
        append(payload, "module", rule.module());
        append(payload, "operation", rule.operation());
        append(payload, "statuses", rule.statuses());
        append(payload, "roles", rule.roles());
        append(payload, "conditions", rule.conditions());
        append(payload, "decision", rule.decision());
        append(payload, "effect", rule.effect());
        append(payload, "title", rule.title());
        append(payload, "keywords", rule.keywords());
        append(payload, "answer", rule.answer());
        append(payload, "safeNextSteps", rule.safeNextSteps());
        append(payload, "prohibited", rule.prohibited());
        append(payload, "effectiveDate", rule.effectiveDate());
        append(payload, "businessOwner", rule.businessOwner());
        append(payload, "reviewer", rule.reviewer());
        append(payload, "source", rule.source());
        append(payload, "priority", Integer.toString(rule.priority()));
        append(payload, "specificity", Integer.toString(rule.specificity()));
        append(payload, "supersedes", rule.supersedes());
        append(payload, "conflictResolutionId", rule.conflictResolutionId());
        return "sha256:" + HexFormat.of().formatHex(digest(payload.toString()));
    }

    private void append(StringBuilder target, String name, String value) {
        target.append(name).append('\u001f').append(value == null ? "<null>" : value).append('\u001d');
    }

    private void append(StringBuilder target, String name, List<String> values) {
        String value = values == null ? "<null>" : String.join("\u001e", values);
        append(target, name, value);
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}

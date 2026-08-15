package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Applies cross-entry safety rules that JSON schema alone cannot express. */
final class ProjectMemoryDocumentInvariantValidator {

    void validate(JsonNode document) {
        Map<String, String> phrases = new HashMap<>();
        JsonNode terms = document.path("terms");
        if (terms.isObject()) {
            terms.fields().forEachRemaining(entry -> validateTerm(entry.getValue(), phrases));
        }
        JsonNode rules = document.path("rules");
        if (rules.isObject() && terms.isObject()) {
            rules.fields().forEachRemaining(rule -> validateAgainstRules(rule.getValue(), terms));
        }
    }

    private void validateTerm(JsonNode term, Map<String, String> phrases) {
        if (!"ACTIVE".equals(term.path("status").asText())) return;
        String intent = term.path("intent").asText("");
        Set<String> values = new HashSet<>();
        add(values, term.path("phrase"));
        term.path("aliases").forEach(node -> add(values, node));
        for (String phrase : values) {
            String previous = phrases.putIfAbsent(phrase, intent);
            if (previous != null && !previous.equals(intent)) {
                throw new IllegalArgumentException("MEMORY_CONFLICT_BLOCKED: " + phrase);
            }
        }
    }

    private void validateAgainstRules(JsonNode rule, JsonNode terms) {
        if (!"ACTIVE".equals(rule.path("status").asText())) return;
        String intent = rule.path("intent").asText("");
        String scope = rule.path("scope").asText("");
        Set<String> keywords = new HashSet<>();
        rule.path("keywords").forEach(node -> add(keywords, node));
        terms.fields().forEachRemaining(term -> {
            JsonNode value = term.getValue();
            if (!"ACTIVE".equals(value.path("status").asText())
                    || !scope.equals(value.path("scope").asText())
                    || intent.equals(value.path("intent").asText())) return;
            Set<String> phrases = new HashSet<>();
            add(phrases, value.path("phrase"));
            value.path("aliases").forEach(node -> add(phrases, node));
            if (phrases.stream().anyMatch(keywords::contains)) {
                throw new IllegalArgumentException("MEMORY_CONFLICT_BLOCKED: " + term.getKey());
            }
        });
    }

    private void add(Set<String> values, JsonNode node) {
        if (node.isTextual() && !node.asText().isBlank()) values.add(node.asText());
    }
}

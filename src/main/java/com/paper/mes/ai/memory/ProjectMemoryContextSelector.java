package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Selects a small, relevant memory slice for model explanation prompts. */
@org.springframework.stereotype.Component
public class ProjectMemoryContextSelector {

    public String select(ProjectMemorySnapshot snapshot, String question, String pageTemplate, int maxChars) {
        return selectWithIds(snapshot, question, pageTemplate, maxChars).context();
    }

    public ProjectMemorySelection selectWithIds(ProjectMemorySnapshot snapshot, String question,
                                                 String pageTemplate, int maxChars) {
        if (snapshot == null || question == null || question.isBlank()) {
            return new ProjectMemorySelection("", List.of());
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        List<Match> matches = new ArrayList<>();
        collect(snapshot.document().path("rules"), normalized, matches);
        collect(snapshot.document().path("terms"), normalized, matches);
        collect(snapshot.document().path("examples"), normalized, matches);
        StringBuilder result = new StringBuilder("memoryVersion=").append(snapshot.docVersion());
        if (pageTemplate != null && !pageTemplate.isBlank()) result.append(" pageTemplate=").append(pageTemplate);
        List<String> selectedIds = new ArrayList<>();
        for (Match match : matches) {
            if (result.length() + match.content().length() + 1 > maxChars) break;
            result.append('\n').append(match.content());
            selectedIds.add(match.id());
        }
        String context = selectedIds.isEmpty() ? "" : result.toString();
        return new ProjectMemorySelection(context, selectedIds);
    }

    private void collect(JsonNode entries, String question, List<Match> matches) {
        if (!entries.isObject()) return;
        entries.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (!"ACTIVE".equals(value.path("status").asText("ACTIVE"))) return;
            if (matchesQuestion(value, question)) {
                matches.add(new Match(entry.getKey(), entry.getKey() + "=" + compact(value)));
            }
        });
    }

    private boolean matchesQuestion(JsonNode entry, String question) {
        if (contains(question, entry.path("phrase").asText(null))
                || contains(question, entry.path("meaning").asText(null))
                || contains(question, entry.path("intent").asText(null))) return true;
        for (JsonNode keyword : entry.path("keywords")) if (contains(question, keyword.asText(null))) return true;
        for (JsonNode alias : entry.path("aliases")) if (contains(question, alias.asText(null))) return true;
        return contains(question, entry.path("input").asText(null));
    }

    private boolean contains(String text, String term) {
        return term != null && !term.isBlank() && text.contains(term.toLowerCase(Locale.ROOT));
    }

    private String compact(JsonNode value) {
        return redact(value).toString().replace("\n", "");
    }

    private JsonNode redact(JsonNode value) {
        if (value.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            value.fields().forEachRemaining(entry -> {
                if (Set.of("price", "unitPrice", "amount", "tax", "apiKey", "phone", "address",
                        "customerName", "orderUuid", "rollUuid", "rollNo").contains(entry.getKey())) return;
                result.set(entry.getKey(), redact(entry.getValue()));
            });
            return result;
        }
        if (value.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            value.forEach(item -> result.add(redact(item)));
            return result;
        }
        if (value.isTextual()) {
            return JsonNodeFactory.instance.textNode(value.asText()
                    .replaceAll("(?<!\\d)\\d+(?:\\.\\d+)?\\s*(?:元|块钱|块|CNY|¥)", "[金额]"));
        }
        return value;
    }

    private record Match(String id, String content) {
    }
}

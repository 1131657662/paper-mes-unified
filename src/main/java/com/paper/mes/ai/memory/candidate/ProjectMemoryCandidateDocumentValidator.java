package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Enforces the narrow candidate shape before an administrator can promote it. */
@Component
class ProjectMemoryCandidateDocumentValidator {

    private static final Set<String> TERM_FIELDS = Set.of(
            "type", "scope", "status", "phrase", "aliases", "intent", "meaning", "source");
    private static final Set<String> EXAMPLE_FIELDS = Set.of(
            "type", "scope", "status", "input", "expected", "evidenceRequired", "source");
    private static final Set<String> EXPECTED_FIELDS = Set.of("processType", "intent", "field");
    private static final Set<String> SOURCES = Set.of(
            "confirmed-ai-candidate", "manual-final-configuration-after-ai-conversation");

    private final ProcessTextRedactor redactor;

    ProjectMemoryCandidateDocumentValidator() {
        this(new ProcessTextRedactor());
    }

    ProjectMemoryCandidateDocumentValidator(ProcessTextRedactor redactor) {
        this.redactor = redactor;
    }

    void validate(String candidateType, JsonNode candidate) {
        if (candidate == null || !candidate.isObject()) invalid();
        Set<String> fields = "TERM".equals(candidateType) ? TERM_FIELDS
                : "EXAMPLE".equals(candidateType) ? EXAMPLE_FIELDS : Set.of();
        if (fields.isEmpty() || !candidate.fieldNames().hasNext()) invalid();
        candidate.fieldNames().forEachRemaining(field -> {
            if (!fields.contains(field)) invalid();
        });
        requireText(candidate, "type", candidateType);
        requireText(candidate, "scope", null);
        requireText(candidate, "status", "ACTIVE");
        requireText(candidate, "source", null);
        if (!SOURCES.contains(candidate.path("source").asText())) invalid();
        if ("TERM".equals(candidateType)) {
            requireText(candidate, "phrase", null);
            requireText(candidate, "intent", null);
            requireText(candidate, "meaning", null);
            if (!candidate.path("aliases").isArray() || candidate.path("aliases").size() > 16) invalid();
            candidate.path("aliases").forEach(alias -> {
                if (!alias.isTextual() || alias.asText().isBlank() || alias.asText().length() > 120) {
                    invalid();
                }
            });
            requireRedacted(candidate);
            return;
        }
        requireText(candidate, "input", null);
        JsonNode expected = candidate.path("expected");
        if (!expected.isObject() || expected.size() != EXPECTED_FIELDS.size()) invalid();
        expected.fieldNames().forEachRemaining(field -> {
            if (!EXPECTED_FIELDS.contains(field)) invalid();
            requireText(expected, field, null);
        });
        if (!candidate.path("evidenceRequired").isBoolean()
                || !candidate.path("evidenceRequired").asBoolean()) invalid();
        requireRedacted(candidate);
    }

    void validateSharedText(JsonNode candidate) {
        if (candidate == null || !candidate.isObject()) invalid();
        requireRedacted(candidate);
    }

    private void requireRedacted(JsonNode node) {
        if (node.isTextual() && !node.asText().isBlank()) {
            if (redactor.redact(node.asText()).modified()) invalid();
            return;
        }
        if (node.isContainerNode()) node.elements().forEachRemaining(this::requireRedacted);
    }

    private void requireText(JsonNode node, String field, String expected) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()
                || value.asText().length() > 500
                || (expected != null && !expected.equals(value.asText()))) invalid();
    }

    private void invalid() {
        throw new BusinessException(ResultCode.BAD_REQUEST,
                "MEMORY_CANDIDATE_EDIT_INVALID", "候选知识字段或内容不符合安全契约");
    }
}

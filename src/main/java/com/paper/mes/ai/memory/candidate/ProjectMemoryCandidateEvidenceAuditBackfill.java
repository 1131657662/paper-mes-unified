package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/** Encrypts legacy evidence before removing its plaintext from shared tables. */
@Slf4j
@Service
class ProjectMemoryCandidateEvidenceAuditBackfill {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_SHAPE_DEPTH = 8;
    private static final int MAX_CONTAINER_ENTRIES = 64;
    private static final String AUDIT_ROLE = "MEMORY_EVIDENCE_AUDIT";

    private final ProjectMemoryCandidateRepository repository;
    private final AiMessageCipher cipher;
    private final ObjectMapper objectMapper;
    private final ProcessTextRedactor redactor;

    ProjectMemoryCandidateEvidenceAuditBackfill(ProjectMemoryCandidateRepository repository,
                                                AiMessageCipher cipher, ObjectMapper objectMapper) {
        this(repository, cipher, objectMapper, new ProcessTextRedactor());
    }

    @Autowired
    ProjectMemoryCandidateEvidenceAuditBackfill(ProjectMemoryCandidateRepository repository,
                                                AiMessageCipher cipher, ObjectMapper objectMapper,
                                                ProcessTextRedactor redactor) {
        this.repository = repository;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
        this.redactor = redactor;
    }

    @Scheduled(initialDelayString = "${app.ai.memory.evidence-audit-backfill-initial-delay-ms:30000}",
            fixedDelayString = "${app.ai.memory.evidence-audit-backfill-ms:60000}")
    public void process() {
        List<LegacyEvidenceAuditContext> contexts = repository.findLegacyAuditContexts(BATCH_SIZE);
        contexts.forEach(context -> {
            try {
                encryptAndClear(context);
            } catch (RuntimeException exception) {
                // One corrupt legacy row must not keep later rows in plaintext forever.
                log.warn("AI memory evidence audit backfill skipped row: evidenceId={}, type={}",
                        context.uuid(), exception.getClass().getSimpleName());
            }
        });
    }

    private void encryptAndClear(LegacyEvidenceAuditContext context) {
        String plaintext = serialize(context);
        String ciphertext = cipher.encrypt(new AiMessageCryptoContext(
                context.uuid(), 0, AUDIT_ROLE), plaintext);
        if (repository.backfillAuditContext(context.uuid(), ciphertext, cipher.hash(plaintext)) != 1) {
            log.warn("AI memory evidence audit context changed before backfill: evidenceId={}",
                    context.uuid());
        }
    }

    private String serialize(LegacyEvidenceAuditContext context) {
        ParsedJson contextJson = parse(context.contextJson());
        ParsedJson proposedValue = parse(context.proposedValueJson());
        ParsedJson finalValue = parse(context.finalValueJson());
        ParsedJson difference = parse(context.differenceJson());
        if (contextJson.malformed() || proposedValue.malformed() || finalValue.malformed()
                || difference.malformed()) {
            log.warn("AI memory evidence audit context is malformed; encrypting sanitized shape: evidenceId={}",
                    context.uuid());
        }
        String phraseHash = context.phrase() == null ? null : cipher.hash(sanitizeText(context.phrase()));
        return json(new AuditContext(phraseHash, shape(contextJson.value(), 0),
                shape(proposedValue.value(), 0), shape(finalValue.value(), 0),
                shape(difference.value(), 0), contextJson.malformed()
                || proposedValue.malformed() || finalValue.malformed() || difference.malformed()));
    }

    private String sanitizeText(String value) {
        if (value == null || value.isBlank()) return "";
        return redactor.redact(value).sanitizedText();
    }

    /** Keep only type/shape and one-way fingerprints; never copy legacy text or values. */
    private JsonNode shape(JsonNode value, int depth) {
        if (value == null || value.isNull()) return null;
        if (depth >= MAX_SHAPE_DEPTH && value.isContainerNode()) {
            return objectMapper.createObjectNode().put("type", "truncated");
        }
        if (value.isTextual()) {
            ObjectNode result = objectMapper.createObjectNode();
            String sanitized = sanitizeText(value.asText());
            result.put("type", "text");
            result.put("length", sanitized.length());
            result.put("hash", cipher.hash(sanitized));
            return result;
        }
        if (value.isNumber()) {
            return objectMapper.createObjectNode().put("type", "number");
        }
        if (value.isBoolean()) {
            return objectMapper.createObjectNode().put("type", "boolean");
        }
        if (value.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            int count = 0;
            for (JsonNode item : value) {
                if (count++ >= MAX_CONTAINER_ENTRIES) break;
                result.add(shape(item, depth + 1));
            }
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("type", "array");
            wrapper.set("items", result);
            if (value.size() > MAX_CONTAINER_ENTRIES) {
                wrapper.put("truncated", true);
                wrapper.put("count", value.size());
            }
            return wrapper;
        }
        if (value.isObject()) {
            ObjectNode fields = objectMapper.createObjectNode();
            int count = 0;
            var entries = value.fields();
            while (entries.hasNext() && count++ < MAX_CONTAINER_ENTRIES) {
                var entry = entries.next();
                fields.set(entry.getKey(), shape(entry.getValue(), depth + 1));
            }
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("type", "object");
            wrapper.set("fields", fields);
            if (value.size() > MAX_CONTAINER_ENTRIES) {
                wrapper.put("truncated", true);
                wrapper.put("count", value.size());
            }
            return wrapper;
        }
        return objectMapper.createObjectNode().put("type", value.getNodeType().name());
    }

    private String json(AuditContext value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("memory evidence audit context serialization failed", exception);
        }
    }

    private ParsedJson parse(String value) {
        if (value == null) return new ParsedJson(null, false);
        try {
            JsonNode parsed = objectMapper.readTree(value);
            return new ParsedJson(parsed, parsed == null);
        } catch (Exception exception) {
            return new ParsedJson(objectMapper.getNodeFactory().textNode(value), true);
        }
    }

    private record ParsedJson(JsonNode value, boolean malformed) {
    }

    private record AuditContext(String phraseHash, JsonNode context, JsonNode proposedValue,
                                JsonNode finalValue, JsonNode difference, boolean malformed) {
    }
}

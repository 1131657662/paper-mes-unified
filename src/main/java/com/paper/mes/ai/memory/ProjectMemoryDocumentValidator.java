package com.paper.mes.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

/** Applies the shared JSON, metadata, size, and checksum contract. */
@Component
@RequiredArgsConstructor
public class ProjectMemoryDocumentValidator {

    public static final int MAX_DOCUMENT_BYTES = 512 * 1024;
    private static final String SUPPORTED_SCHEMA_VERSION = "1.0";
    private static final String SUPPORTED_SCHEMA_ID = "paper-mes-project-memory-1.0";
    private static final Pattern CHECKSUM_PATTERN = Pattern.compile("sha256:[0-9a-f]{64}");

    private final ObjectMapper objectMapper;
    private final ProjectMemoryChecksum checksum;

    public ProjectMemorySnapshot validateDatabaseRow(ProjectMemoryDocumentRow row) {
        if (!"ACTIVE".equals(row.status())) {
            throw new IllegalArgumentException("project memory row is not ACTIVE");
        }
        JsonNode document = parse(row.docJson());
        return validate(document, row.docVersion(), row.schemaVersion(), row.checksum());
    }

    public ProjectMemorySnapshot validateSeed(JsonNode document) {
        String docVersion = requiredText(document, "memoryVersion");
        String schemaVersion = requiredText(document, "schemaVersion");
        String declaredChecksum = requiredText(document, "checksum");
        return validate(document, docVersion, schemaVersion, declaredChecksum);
    }

    public ProjectMemorySnapshot validateNode(JsonNode document, String docVersion,
                                              String schemaVersion, String checksum) {
        return validate(document, docVersion, schemaVersion, checksum);
    }

    private ProjectMemorySnapshot validate(JsonNode document, String expectedDocVersion,
                                           String expectedSchemaVersion, String expectedChecksum) {
        if (document == null || !document.isObject()) {
            throw new IllegalArgumentException("project memory document must be a JSON object");
        }
        ensureLength(expectedDocVersion, 32, "doc_version");
        ensureLength(expectedSchemaVersion, 16, "schema_version");
        if (!SUPPORTED_SCHEMA_VERSION.equals(expectedSchemaVersion)
                || !SUPPORTED_SCHEMA_ID.equals(document.path("$schema").asText(null))) {
            throw new IllegalArgumentException("project memory schema version is unsupported");
        }
        ensureChecksum(expectedChecksum);
        String documentVersion = requiredText(document, "memoryVersion");
        String documentSchema = requiredText(document, "schemaVersion");
        String documentChecksum = requiredText(document, "checksum");
        if (!expectedDocVersion.equals(documentVersion)
                || !expectedSchemaVersion.equals(documentSchema)
                || !expectedChecksum.equals(documentChecksum)) {
            throw new IllegalArgumentException("project memory database metadata does not match JSON metadata");
        }
        validateEntryShape(document);
        try {
            if (objectMapper.writeValueAsString(document).getBytes(StandardCharsets.UTF_8).length
                    > MAX_DOCUMENT_BYTES) {
                throw new IllegalArgumentException("project memory document exceeds 512KB");
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("project memory document cannot be serialized", ex);
        }
        checksum.requireValid(document);
        return new ProjectMemorySnapshot(expectedDocVersion, expectedSchemaVersion,
                expectedChecksum, document, Instant.now());
    }

    private void validateEntryShape(JsonNode document) {
        for (String category : new String[]{"rules", "terms", "examples", "disabled"}) {
            JsonNode entries = document.path(category);
            if (!entries.isObject()) throw new IllegalArgumentException("project memory category is invalid: " + category);
            entries.fields().forEachRemaining(entry -> {
                if (!entry.getValue().isObject()) {
                    throw new IllegalArgumentException("project memory entry is invalid: " + entry.getKey());
                }
                JsonNode status = entry.getValue().get("status");
                if (!"disabled".equals(category) && status == null) {
                    throw new IllegalArgumentException("project memory entry status is required: " + entry.getKey());
                }
                if (status != null && (!status.isTextual() || !Set.of("ACTIVE", "INACTIVE").contains(status.asText()))) {
                    throw new IllegalArgumentException("project memory entry status is invalid: " + entry.getKey());
                }
            });
        }
    }

    private JsonNode parse(String json) {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException("project memory document exceeds 512KB");
        }
        try {
            return objectMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("project memory document is invalid JSON", ex);
        }
    }

    private String requiredText(JsonNode document, String field) {
        String value = document == null ? null : document.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("project memory field is required: " + field);
        }
        return value;
    }

    private void ensureChecksum(String value) {
        if (value == null || !CHECKSUM_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("project memory checksum format is invalid");
        }
    }

    private void ensureLength(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("project memory " + field + " is invalid");
        }
    }
}

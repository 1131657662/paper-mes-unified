package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Calculates the checksum declared by the project-memory JSON contract. */
@Component
public final class ProjectMemoryChecksum {

    private static final String CHECKSUM_FIELD = "checksum";
    private static final String PREFIX = "sha256:";

    private final ObjectMapper objectMapper;

    public ProjectMemoryChecksum(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String calculate(JsonNode document) {
        Objects.requireNonNull(document, "document");
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(canonicalize(document, true));
            return PREFIX + HexFormat.of().formatHex(sha256(canonical));
        } catch (IOException ex) {
            throw new IllegalArgumentException("项目记忆文档无法规范化", ex);
        }
    }

    public boolean matches(JsonNode document) {
        String expected = document == null ? null : document.path(CHECKSUM_FIELD).asText(null);
        return expected != null && expected.equals(calculate(document));
    }

    public void requireValid(JsonNode document) {
        Objects.requireNonNull(document, "document");
        String expected = document.path(CHECKSUM_FIELD).asText(null);
        String actual = calculate(document);
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException("项目记忆 checksum 不匹配：expected=" + expected
                    + ", actual=" + actual);
        }
    }

    private JsonNode canonicalize(JsonNode node, boolean root) {
        if (node.isObject()) return canonicalObject((ObjectNode) node, root);
        if (node.isArray()) return canonicalArray((ArrayNode) node);
        return node;
    }

    private ObjectNode canonicalObject(ObjectNode source, boolean root) {
        ObjectNode result = objectMapper.createObjectNode();
        List<String> names = new ArrayList<>();
        source.fieldNames().forEachRemaining(names::add);
        names.removeIf(name -> root && CHECKSUM_FIELD.equals(name));
        names.sort(ProjectMemoryChecksum::compareCodePoints);
        for (String name : names) result.set(name, canonicalize(source.get(name), false));
        return result;
    }

    private ArrayNode canonicalArray(ArrayNode source) {
        ArrayNode result = objectMapper.createArrayNode();
        source.forEach(item -> result.add(canonicalize(item, false)));
        return result;
    }

    private static int compareCodePoints(String left, String right) {
        int[] leftPoints = left.codePoints().toArray();
        int[] rightPoints = right.codePoints().toArray();
        int length = Math.min(leftPoints.length, rightPoints.length);
        for (int index = 0; index < length; index++) {
            if (leftPoints[index] != rightPoints[index]) {
                return Integer.compare(leftPoints[index], rightPoints[index]);
            }
        }
        return Integer.compare(leftPoints.length, rightPoints.length);
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256不可用", ex);
        }
    }

}

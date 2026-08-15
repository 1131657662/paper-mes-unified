package com.paper.mes.ai.memory;

import com.paper.mes.ai.memory.dto.ProjectMemoryPatchOperation;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Enforces the server-side RFC 6902 subset and path allowlist. */
final class ProjectMemoryPatchPolicy {

    private static final Set<String> OPERATIONS = Set.of("add", "replace", "remove");
    private static final Set<String> ROOTS = Set.of("rules", "terms", "examples", "disabled");
    private static final Set<String> FIELDS = Set.of(
            "status", "intent", "content", "keywords", "aliases", "meaning", "source", "scope",
            "phrase", "input", "notMeaning", "constraints", "targetDiameter", "defaultValue", "defaultUnit",
            "ratios", "parts", "stepType", "billing", "expected", "redactedInput", "unit", "value",
            "min", "max", "requiresBackendBranch", "modelOutput", "backendResult", "ownerRollRef",
            "sourceRollRefs", "coveredRollRefs", "weightAllocation", "remainderPolicy", "remainderDirection",
            "businessConfirmed", "maxPieceWeightDifferenceKg", "evidenceRequired", "autoApply");
    private static final Pattern ENTRY = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    private ProjectMemoryPatchPolicy() {
    }

    static List<String> validate(ProjectMemoryPatchOperation operation) {
        if (operation == null || operation.op() == null || operation.path() == null) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID");
        }
        if (!OPERATIONS.contains(operation.op())) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID");
        }
        List<String> parts = pointer(operation.path());
        if (parts.size() < 2 || !ROOTS.contains(parts.getFirst()) || !ENTRY.matcher(parts.get(1)).matches()) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID_PATH");
        }
        if (parts.size() == 2 && "remove".equals(operation.op())) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID_PATH");
        }
        if (parts.subList(2, parts.size()).stream().anyMatch(part -> !FIELDS.contains(part)
                && !part.matches("\\d+") && !"-".equals(part))) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID_FIELD");
        }
        boolean valueRequired = "add".equals(operation.op()) || "replace".equals(operation.op());
        if (valueRequired != (operation.value() != null)) {
            throw new IllegalArgumentException("MEMORY_PATCH_VALUE_REQUIRED");
        }
        return parts;
    }

    static List<String> pointer(String path) {
        if (path.isBlank() || !path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID_PATH");
        }
        return java.util.Arrays.stream(path.substring(1).split("/", -1))
                .map(ProjectMemoryPatchPolicy::decode)
                .toList();
    }

    private static String decode(String segment) {
        if (segment.contains("~") && !segment.matches("(?:[^~]|~[01])*$")) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID_POINTER");
        }
        return segment.replace("~1", "/").replace("~0", "~");
    }
}

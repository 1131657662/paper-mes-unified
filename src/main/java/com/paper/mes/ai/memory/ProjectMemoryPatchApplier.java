package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchOperation;

import java.util.List;

/** Applies the allowlisted add, replace, and remove JSON Patch operations. */
final class ProjectMemoryPatchApplier {

    ObjectNode apply(ObjectNode source, List<ProjectMemoryPatchOperation> operations) {
        ObjectNode result = source.deepCopy();
        for (ProjectMemoryPatchOperation operation : operations) {
            List<String> path = ProjectMemoryPatchPolicy.validate(operation);
            mutate(result, path, operation);
        }
        return result;
    }

    private void mutate(ObjectNode root, List<String> path, ProjectMemoryPatchOperation operation) {
        JsonNode parent = parent(root, path);
        String name = path.getLast();
        if (parent instanceof ObjectNode object) {
            mutateObject(object, name, operation);
            return;
        }
        if (parent instanceof ArrayNode array) {
            mutateArray(array, name, operation);
            return;
        }
        throw new IllegalArgumentException("MEMORY_PATCH_PARENT_NOT_CONTAINER");
    }

    private JsonNode parent(JsonNode root, List<String> path) {
        JsonNode current = root;
        for (int index = 0; index < path.size() - 1; index++) {
            String part = path.get(index);
            current = current instanceof ObjectNode object ? object.get(part) : arrayItem(current, part);
            if (current == null) {
                throw new IllegalArgumentException("MEMORY_PATCH_PATH_NOT_FOUND");
            }
        }
        return current;
    }

    private JsonNode arrayItem(JsonNode node, String part) {
        if (!(node instanceof ArrayNode array) || "-".equals(part) || !part.matches("\\d+")) {
            throw new IllegalArgumentException("MEMORY_PATCH_INVALID_ARRAY_PATH");
        }
        int index = Integer.parseInt(part);
        if (index >= array.size()) {
            throw new IllegalArgumentException("MEMORY_PATCH_PATH_NOT_FOUND");
        }
        return array.get(index);
    }

    private void mutateObject(ObjectNode object, String name, ProjectMemoryPatchOperation operation) {
        boolean exists = object.has(name);
        if ("replace".equals(operation.op()) && !exists || "remove".equals(operation.op()) && !exists) {
            throw new IllegalArgumentException("MEMORY_PATCH_PATH_NOT_FOUND");
        }
        if ("remove".equals(operation.op())) {
            object.remove(name);
        } else {
            object.set(name, operation.value().deepCopy());
        }
    }

    private void mutateArray(ArrayNode array, String name, ProjectMemoryPatchOperation operation) {
        int index = "-".equals(name) ? array.size() : parseIndex(name, array.size());
        if ("add".equals(operation.op())) {
            array.insert(index, operation.value().deepCopy());
        } else {
            if (index >= array.size()) {
                throw new IllegalArgumentException("MEMORY_PATCH_PATH_NOT_FOUND");
            }
            if ("remove".equals(operation.op())) array.remove(index);
            else array.set(index, operation.value().deepCopy());
        }
    }

    private int parseIndex(String value, int size) {
        if (!value.matches("\\d+")) throw new IllegalArgumentException("MEMORY_PATCH_INVALID_ARRAY_PATH");
        int index = Integer.parseInt(value);
        if (index > size) throw new IllegalArgumentException("MEMORY_PATCH_PATH_NOT_FOUND");
        return index;
    }
}

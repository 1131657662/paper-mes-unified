package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessStepRouteMutationContractTest {

    private static final Path MAPPER = Path.of(
            "src/main/java/com/paper/mes/processorder/mapper/ProcessStepMapper.java");
    private static final Path SERVICE = Path.of(
            "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java");

    @Test
    void routeReferenceQuery_coversEveryActiveTopologyEdge() throws IOException {
        String source = read(MAPPER);

        assertContainsAll(source,
                "output.is_deleted = 0 AND output.step_uuid = #{stepUuid}",
                "input_rel.is_deleted = 0",
                "input_rel.step_uuid = #{stepUuid}",
                "input_rel.source_step_uuid = #{stepUuid}",
                "child.is_deleted = 0 AND child.parent_step_uuid = #{stepUuid}");
    }

    @Test
    void ordinaryStepCommands_callRouteGuardBeforeMutation() throws IOException {
        String source = read(SERVICE);
        String update = slice(source, "public void updateProcessStep", "public void deleteProcessStep");
        String delete = slice(source, "public void deleteProcessStep", "private void validateAddStepStatus");

        assertPrecedes(update,
                "processStepRouteMutationGuard.requireOrdinaryMutationAllowed(step)",
                "step.setStepType(dto.getStepType())");
        assertPrecedes(delete,
                "processStepRouteMutationGuard.requireOrdinaryMutationAllowed(step)",
                "processStepMapper.deleteById(stepUuid)");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "Missing start marker: " + start);
        assertTrue(endIndex >= 0, "Missing end marker: " + end);
        return source.substring(startIndex, endIndex);
    }

    private void assertContainsAll(String source, String... snippets) {
        for (String snippet : snippets) {
            assertTrue(source.contains(snippet), "Missing snippet: " + snippet);
        }
    }

    private void assertPrecedes(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing guard: " + first);
        assertTrue(secondIndex > firstIndex, "Mutation must follow route guard: " + second);
    }
}

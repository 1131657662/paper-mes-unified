package com.paper.mes.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinishRollNumberFreezeContractTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/paper/mes/processorder/service/impl/FinishRollServiceImpl.java");

    @ParameterizedTest
    @ValueSource(strings = {"batchGenerate", "appendSpare", "voidRollNo", "batchVoidRollNo"})
    void ordinaryRollNumberCommands_requirePendingOrder(String methodName) throws IOException {
        String source = read();
        String method = slicePublicMethod(source, methodName);

        assertTrue(method.contains("requireRollNumberEditable"),
                methodName + " must enforce the issued-plan freeze");
    }

    @Test
    void rollNumberPolicy_allowsOnlyPendingStatus() throws IOException {
        String source = read();
        String policy = slice(source, "private void requireRollNumberEditable", "    }\n}");

        assertTrue(policy.contains("Integer.valueOf(STATUS_PENDING).equals(status)"));
        assertFalse(policy.contains("STATUS_PROCESSING"));
        assertFalse(policy.contains("STATUS_TO_RECORD"));
        assertTrue(policy.contains("重新下发或回录命令"));
    }

    private String read() throws IOException {
        return Files.readString(SERVICE, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private String slicePublicMethod(String source, String methodName) {
        int methodIndex = source.indexOf(methodName);
        int startIndex = source.lastIndexOf("public ", methodIndex);
        int endIndex = source.indexOf("private ", methodIndex);
        assertTrue(startIndex >= 0, "Missing method: " + methodName);
        assertTrue(endIndex >= 0, "Missing end marker for: " + methodName);
        return source.substring(startIndex, endIndex);
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        assertTrue(startIndex >= 0, "Missing start marker: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(endIndex >= 0, "Missing end marker: " + end);
        return source.substring(startIndex, endIndex);
    }
}

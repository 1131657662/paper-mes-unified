package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessOrderCompletionPrintGateContractTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java");

    @Test
    void completionCommand_usesUnifiedStatusTransition() throws IOException {
        String method = method(source(), "public void completeProcessing", "public void rollbackStatus");

        assertTrue(method.contains("changeStatus(uuid, OrderStatus.TO_RECORD.getCode(), reason)"));
    }

    @Test
    void processingToRecord_requiresConfirmedPrintBeforeWrite() throws IOException {
        String method = method(source(), "public void changeStatus", "public void markSettled");

        assertTrue(method.contains("from == OrderStatus.PROCESSING"));
        assertTrue(method.contains("to == OrderStatus.TO_RECORD"));
        assertTrue(method.contains("order.getPrintCount() == null"));
        assertTrue(method.contains("order.getPrintCount() <= 0"));
        assertTrue(method.contains("人工确认一次打印"));
        assertTrue(method.contains("不代表打印机设备回执"));
        assertTrue(method.indexOf("order.getPrintCount()") < method.indexOf("order.setOrderStatus"));
    }

    private String source() throws IOException {
        return Files.readString(SERVICE, StandardCharsets.UTF_8);
    }

    private String method(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "missing method start: " + start);
        assertTrue(endIndex > startIndex, "missing method end: " + end);
        return source.substring(startIndex, endIndex);
    }
}

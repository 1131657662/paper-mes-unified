package com.paper.mes.system.config.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentNoUsageContractTest {

    private static final List<String> SERVICE_FILES = List.of(
            "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java",
            "src/main/java/com/paper/mes/processorder/service/RollNoSequenceService.java",
            "src/main/java/com/paper/mes/delivery/service/impl/DeliveryServiceImpl.java",
            "src/main/java/com/paper/mes/settle/service/impl/SettleServiceImpl.java",
            "src/main/java/com/paper/mes/customer/service/impl/CustomerServiceImpl.java",
            "src/main/java/com/paper/mes/paper/service/impl/PaperServiceImpl.java",
            "src/main/java/com/paper/mes/machine/service/impl/MachineServiceImpl.java",
            "src/main/java/com/paper/mes/warehouse/service/impl/WarehouseServiceImpl.java"
    );

    @Test
    void coreServices_whenGeneratingNumbers_useDocumentNoService() throws IOException {
        for (String file : SERVICE_FILES) {
            assertTrue(source(file).contains("documentNoService.next"), file + " should call DocumentNoService");
        }
    }

    @Test
    void coreServices_whenGeneratingNumbers_doNotUseLegacyPrefixConcatenation() throws IOException {
        for (String file : SERVICE_FILES) {
            String source = source(file);
            assertFalse(source.contains("\"JG\" +"), file + " must not hardcode process order prefix");
            assertFalse(source.contains("\"CK\" +"), file + " must not hardcode delivery order prefix");
            assertFalse(source.contains("\"JS\" +"), file + " must not hardcode settle order prefix");
            assertFalse(source.contains("\"A\" +"), file + " must not hardcode finish roll prefix");
        }
    }

    private String source(String file) throws IOException {
        return Files.readString(Path.of(file), StandardCharsets.UTF_8);
    }
}

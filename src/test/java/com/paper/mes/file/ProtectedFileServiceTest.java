package com.paper.mes.file;

import com.paper.mes.common.BusinessException;
import com.paper.mes.config.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedFileServiceTest {

    @TempDir
    Path uploadDir;

    @Test
    void load_withStoredFilePath_returnsResource() throws IOException {
        Path image = uploadDir.resolve("damage/20260707/sample.png");
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[] {1, 2, 3});

        ProtectedFileService.ProtectedFile file = service().load("/files/damage/20260707/sample.png");

        assertTrue(file.resource().exists());
    }

    @Test
    void load_withApiFilePath_returnsResource() throws IOException {
        Path image = uploadDir.resolve("damage/20260707/sample.png");
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[] {1, 2, 3});

        ProtectedFileService.ProtectedFile file = service().load("/api/files/damage/20260707/sample.png");

        assertTrue(file.resource().exists());
    }

    @Test
    void load_withTraversalPath_throwsBusinessException() {
        ProtectedFileService service = service();

        assertThrows(BusinessException.class, () -> service.load("../secret.txt"));
    }

    @Test
    void load_withEncodedTraversalPath_throwsBusinessException() {
        ProtectedFileService service = service();

        assertThrows(BusinessException.class, () -> service.load("%2e%2e/secret.txt"));
    }

    private ProtectedFileService service() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDir(uploadDir.toString());
        return new ProtectedFileService(properties);
    }
}

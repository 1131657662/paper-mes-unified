package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.config.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceImplTest {

    @TempDir
    Path uploadDir;

    @Test
    void store_withPngSignature_storesProtectedUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "damage.png", "image/png", pngBytes());

        String url = service().store(file);

        assertTrue(url.startsWith("/api/files/damage/"));
    }

    @Test
    void store_withMismatchedSignature_rejectsFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "damage.png", "image/png", "not-an-image".getBytes());

        assertThrows(BusinessException.class, () -> service().store(file));
    }

    @Test
    void delete_withStoredUrl_removesFile() throws Exception {
        FileStorageServiceImpl service = service();
        String url = service.store(new MockMultipartFile(
                "file", "damage.png", "image/png", pngBytes()));
        Path storedFile;
        try (var paths = java.nio.file.Files.walk(uploadDir)) {
            storedFile = paths.filter(java.nio.file.Files::isRegularFile).findFirst().orElseThrow();
        }

        service.delete(url);

        assertFalse(java.nio.file.Files.exists(storedFile));
    }

    @Test
    void delete_withTraversalPath_rejectsRequest() {
        assertThrows(BusinessException.class, () -> service().delete("/api/files/../outside.png"));
    }

    private FileStorageServiceImpl service() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDir(uploadDir.toString());
        return new FileStorageServiceImpl(properties);
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    }
}

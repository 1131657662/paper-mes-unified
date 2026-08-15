package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProjectMemoryDocumentProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectMemoryChecksum checksum = new ProjectMemoryChecksum(objectMapper);
    private final ProjectMemoryDocumentValidator validator =
            new ProjectMemoryDocumentValidator(objectMapper, checksum);

    @Test
    void loadsActiveSnapshotAndWritesRebuildableCache(@TempDir Path tempDir) throws Exception {
        JsonNode seed = objectMapper.readTree(Files.readString(
                Path.of("docs/ai/project-memory.seed.v1.json"), StandardCharsets.UTF_8));
        ProjectMemoryDocumentRow row = row(seed);
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findActive()).thenReturn(Optional.of(row));
        AiProperties properties = properties(tempDir);
        ProjectMemoryDocumentProvider provider =
                new ProjectMemoryDocumentProvider(properties, objectMapper, repository, validator);

        assertThat(provider.reload()).isPresent();
        assertThat(provider.ready()).isTrue();
        assertThat(Files.exists(provider.cachePath())).isTrue();
        assertThat(objectMapper.readTree(Files.readString(provider.cachePath())))
                .isEqualTo(seed);
    }

    @Test
    void invalidCacheIsRebuiltFromTheDatabaseSnapshot(@TempDir Path tempDir) throws Exception {
        JsonNode seed = objectMapper.readTree(Files.readString(
                Path.of("docs/ai/project-memory.seed.v1.json"), StandardCharsets.UTF_8));
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findActive()).thenReturn(Optional.of(row(seed)));
        ProjectMemoryDocumentProvider provider =
                new ProjectMemoryDocumentProvider(properties(tempDir), objectMapper, repository, validator);
        provider.reload();
        Files.writeString(provider.cachePath(), "{\"tampered\":true}", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(provider.cachePath(), FileTime.from(Instant.now().plusSeconds(2)));

        provider.poll();

        assertThat(objectMapper.readTree(Files.readString(provider.cachePath())))
                .isEqualTo(seed);
        assertThat(provider.ready()).isTrue();
    }

    @Test
    void noActiveSnapshotFailsClosed() {
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findActive()).thenReturn(Optional.empty());
        ProjectMemoryDocumentProvider provider =
                new ProjectMemoryDocumentProvider(properties(Path.of("target/memory-test")),
                        objectMapper, repository, validator);

        assertThat(provider.reload()).isEmpty();
        assertThat(provider.ready()).isFalse();
        assertThat(provider.state()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void databaseChecksumMismatchFailsClosed(@TempDir Path tempDir) throws Exception {
        JsonNode seed = objectMapper.readTree(Files.readString(
                Path.of("docs/ai/project-memory.seed.v1.json"), StandardCharsets.UTF_8));
        ProjectMemoryDocumentRow valid = row(seed);
        ProjectMemoryDocumentRow invalid = new ProjectMemoryDocumentRow(
                valid.uuid(), valid.docVersion(), valid.schemaVersion(), "sha256:" + "0".repeat(64),
                valid.docJson(), valid.status(), valid.patchNotes(), valid.createdBy(), valid.approvedBy());
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findActive()).thenReturn(Optional.of(invalid));
        ProjectMemoryDocumentProvider provider =
                new ProjectMemoryDocumentProvider(properties(tempDir), objectMapper, repository, validator);

        assertThat(provider.reload()).isEmpty();
        assertThat(provider.ready()).isFalse();
    }

    private AiProperties properties(Path memoryDir) {
        AiProperties properties = new AiProperties();
        properties.setMemoryDir(memoryDir.toString());
        return properties;
    }

    private ProjectMemoryDocumentRow row(JsonNode seed) throws Exception {
        return new ProjectMemoryDocumentRow(
                "memory-uuid", seed.path("memoryVersion").asText(), seed.path("schemaVersion").asText(),
                seed.path("checksum").asText(), objectMapper.writeValueAsString(seed), "ACTIVE",
                "test", "system", null);
    }
}

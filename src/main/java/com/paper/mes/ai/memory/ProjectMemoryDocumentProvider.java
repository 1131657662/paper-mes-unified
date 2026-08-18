package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/** Serves the active DB snapshot and maintains its rebuildable local cache. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemoryDocumentProvider {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final ProjectMemoryDocumentRepository repository;
    private final ProjectMemoryDocumentValidator validator;

    private volatile ProjectMemorySnapshot snapshot;
    private volatile String state = "UNAVAILABLE";
    private volatile long cacheLastModified = -1L;

    @PostConstruct
    void initialize() {
        reload();
    }

    public Optional<ProjectMemorySnapshot> current() {
        ProjectMemorySnapshot current = snapshot;
        return current == null ? reload() : Optional.of(current);
    }

    public Optional<ProjectMemorySnapshot> version(String docVersion) {
        ProjectMemorySnapshot current = snapshot;
        if (current != null && current.docVersion().equals(docVersion)) return Optional.of(current);
        try {
            return repository.findVersion(docVersion).map(validator::validateConversationVersion);
        } catch (RuntimeException ex) {
            log.error("Project memory version unavailable for active AI conversation: version={}, type={}",
                    docVersion, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public boolean ready() {
        return snapshot != null;
    }

    public String state() {
        return state;
    }

    public Path cachePath() {
        return Path.of(properties.getMemoryDir()).resolve("project-memory.json");
    }

    public synchronized Optional<ProjectMemorySnapshot> reload() {
        try {
            Optional<ProjectMemoryDocumentRow> row = repository.findActive();
            if (row.isEmpty()) {
                failClosed("no ACTIVE project memory snapshot");
                return Optional.empty();
            }
            loadRow(row.get());
            return Optional.ofNullable(snapshot);
        } catch (RuntimeException ex) {
            failClosed(ex.getMessage());
            return Optional.empty();
        }
    }

    @Scheduled(fixedDelayString = "${app.ai.memory-cache-poll-ms:60000}")
    public synchronized void poll() {
        try {
            Optional<ProjectMemoryDocumentRow> row = repository.findActive();
            if (row.isEmpty()) {
                failClosed("no ACTIVE project memory snapshot");
                return;
            }
            if (snapshot == null || !sameMetadata(row.get(), snapshot)) {
                loadRow(row.get());
                return;
            }
            verifyAndRebuildCache();
        } catch (RuntimeException ex) {
            failClosed(ex.getMessage());
        }
    }

    private void loadRow(ProjectMemoryDocumentRow row) {
        ProjectMemorySnapshot loaded = validator.validateDatabaseRow(row);
        snapshot = loaded;
        state = "READY";
        cacheLastModified = -1L;
        verifyAndRebuildCache(loaded);
    }

    private void verifyAndRebuildCache() {
        verifyAndRebuildCache(snapshot);
    }

    private void verifyAndRebuildCache(ProjectMemorySnapshot value) {
        Path cache = cachePath();
        try {
            if (!Files.exists(cache)) {
                log.info("Project memory cache missing; creating it from the DB snapshot");
                writeCache(value);
                return;
            }
            if (Files.size(cache) > ProjectMemoryDocumentValidator.MAX_DOCUMENT_BYTES) {
                throw new IOException("cache exceeds 512KB");
            }
            long modified = Files.getLastModifiedTime(cache).toMillis();
            if (!"READY".equals(state) || modified != cacheLastModified) {
                JsonNode cached = objectMapper.readTree(Files.readString(cache, StandardCharsets.UTF_8));
                validator.validateNode(cached, value.docVersion(), value.schemaVersion(), value.checksum());
                cacheLastModified = modified;
                state = "READY";
            }
        } catch (Exception ex) {
            log.error("Project memory cache invalid; rebuilding from DB snapshot: {}", ex.getMessage());
            writeCache(value);
        }
    }

    private void writeCache(ProjectMemorySnapshot value) {
        Path cache = cachePath();
        Path temporary = null;
        try {
            Files.createDirectories(cache.getParent());
            temporary = Files.createTempFile(cache.getParent(), ".project-memory-", ".tmp");
            Files.writeString(temporary, objectMapper.writeValueAsString(value.document()), StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, cache, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING);
            }
            cacheLastModified = Files.getLastModifiedTime(cache).toMillis();
            state = "READY";
        } catch (IOException ex) {
            state = "DEGRADED";
            log.error("Project memory cache could not be written: {}", ex.getMessage());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ex) {
                    log.warn("Could not clean temporary project memory cache file", ex);
                }
            }
        }
    }

    private boolean sameMetadata(ProjectMemoryDocumentRow row, ProjectMemorySnapshot current) {
        return row.docVersion().equals(current.docVersion())
                && row.schemaVersion().equals(current.schemaVersion())
                && row.checksum().equals(current.checksum());
    }

    private void failClosed(String reason) {
        snapshot = null;
        state = "UNAVAILABLE";
        log.error("Project memory unavailable; AI memory use is fail-closed: {}", reason);
    }
}

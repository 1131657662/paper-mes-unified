package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

/** Imports the checked-in seed once, without replacing an existing memory history. */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ProjectMemorySeedImporter implements ApplicationRunner {

    private static final String LOCK_NAME = "paper_mes_ai_project_memory_seed";

    private final AiProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ProjectMemoryDocumentRepository repository;
    private final ProjectMemoryDocumentValidator validator;
    private final ProjectMemoryDocumentProvider provider;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            if (seedIfEmpty()) {
                log.info("Initial project memory seed imported");
            }
            provider.reload();
        } catch (DataAccessException | IllegalArgumentException | IOException ex) {
            log.error("Project memory seed unavailable; memory AI will fail closed: {}", ex.getMessage());
        }
    }

    boolean seedIfEmpty() throws IOException {
        Integer lock = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 10)", Integer.class, LOCK_NAME);
        if (lock == null || lock != 1) {
            throw new IllegalStateException("project memory seed lock could not be acquired");
        }
        try {
            if (repository.hasAnyDocuments()) {
                return false;
            }
            JsonNode seed = readSeed();
            ProjectMemorySnapshot snapshot = validator.validateSeed(seed);
            String json = objectMapper.writeValueAsString(snapshot.document());
            repository.insert(new ProjectMemoryDocumentRow(
                    UUID.randomUUID().toString(), snapshot.docVersion(), snapshot.schemaVersion(),
                    snapshot.checksum(), json, "ACTIVE", "Initial checked-in project memory seed",
                    "system", null));
            return true;
        } finally {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, LOCK_NAME);
        }
    }

    private JsonNode readSeed() throws IOException {
        Resource resource = resourceLoader.getResource(properties.getMemorySeedResource());
        if (!resource.exists()) {
            throw new IOException("project memory seed resource does not exist: "
                    + properties.getMemorySeedResource());
        }
        return objectMapper.readTree(resource.getInputStream());
    }
}

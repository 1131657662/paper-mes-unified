package com.paper.mes.ai.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class AiRuleCatalog {

    private final AiProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final AiRuleValidator validator;
    private volatile AiRuleArtifact artifact;
    private volatile String loadError;

    public AiRuleCatalog(AiProperties properties, ResourceLoader resourceLoader,
                         ObjectMapper objectMapper, AiRuleValidator validator) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostConstruct
    void load() {
        try {
            AiRuleArtifact loaded = readArtifact();
            validator.validate(loaded);
            artifact = loaded;
            loadError = null;
            log.info("AI rules loaded: version={}, count={}", loaded.artifactVersion(), loaded.rules().size());
        } catch (RuntimeException | IOException ex) {
            loadError = ex.getMessage();
            log.error("AI rules unavailable; AI module will fail closed: {}", ex.getMessage());
        }
    }

    public List<AiRule> rules() {
        return artifact == null ? List.of() : artifact.rules();
    }

    public String version() {
        return artifact == null ? "unavailable" : artifact.artifactVersion();
    }

    public boolean ready() {
        return artifact != null && loadError == null;
    }

    private AiRuleArtifact readArtifact() throws IOException {
        return objectMapper.readValue(
                resourceLoader.getResource(properties.getRulesResource()).getInputStream(),
                AiRuleArtifact.class);
    }
}

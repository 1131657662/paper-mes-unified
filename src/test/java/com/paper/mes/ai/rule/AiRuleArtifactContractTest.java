package com.paper.mes.ai.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiRuleArtifactContractTest {

    @Test
    void publishedArtifactPassesIntegrityAndConflictChecks() throws Exception {
        String configured = System.getProperty("paper.mes.ai.rules.file",
                "src/main/resources/ai/rules-v1.0.0.json");
        AiRuleArtifact artifact = new ObjectMapper().readValue(Path.of(configured).toFile(), AiRuleArtifact.class);
        AiRuleValidator validator = new AiRuleValidator(new AiRuleConflictDetector(), new AiRuleChecksum());

        validator.validate(artifact);
        assertThat(artifact.rules()).allSatisfy(rule ->
                assertThat(Path.of(rule.source()))
                        .as("规则 %s 的依据文件必须存在", rule.ruleId())
                        .isRegularFile());
    }
}

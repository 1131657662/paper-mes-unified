package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.model.ProcessAiModelResult;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseStoreServiceTest {

    @Test
    void storePersistsAnImmutableProCandidateWithMemoryMetadata() {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        ProcessAiIntentCipher cipher = ProcessAiConfirmationTestFixtures.intentCipher();
        ProcessAiParseStoreService service = new ProcessAiParseStoreService(
                repository, new ObjectMapper(), cipher);
        when(repository.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        ProcessAiParseRecord result = service.store(command());

        ArgumentCaptor<ProcessAiParseRecord> captor = ArgumentCaptor.forClass(ProcessAiParseRecord.class);
        verify(repository).insert(captor.capture());
        assertThat(result.parseRevision()).isEqualTo(2);
        assertThat(captor.getValue().provider()).isEqualTo("DEEPSEEK");
        assertThat(captor.getValue().route()).isEqualTo("PRO");
        assertThat(captor.getValue().projectMemoryItemIds()).isEqualTo("[\"rule-saw\"]");
        assertThat(captor.getValue().resultHash()).hasSize(64);
        assertThat(captor.getValue().intentJson()).contains("aes-gcm-v1");
        assertThat(cipher.decrypt("conversation-1", 2, captor.getValue().intentJson()))
                .contains("parse-1");
    }

    private ProcessAiParseStoreCommand command() {
        ProcessAiSawIntent saw = new ProcessAiSawIntent("CUTS", 2, null, null);
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null, saw, null,
                List.of(new ProcessAiEvidence("knifeCount", "切2刀")));
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemorySnapshot memory = new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:" + "a".repeat(64),
                mapper.createObjectNode(), Instant.now());
        ProcessAiModelResult model = new ProcessAiModelResult(
                "{}", "deepseek-v4-pro-202608", "DEEPSEEK", "PRO", 100, 20);
        return new ProcessAiParseStoreCommand("order-1", "conversation-1", 7,
                2, 1, "request-1", "READY", memory, List.of("rule-saw"), model, extraction);
    }
}

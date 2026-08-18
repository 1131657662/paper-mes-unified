package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMemoryCandidateCaptureServiceTest {

    @Test
    void captureMarksCandidateReadyAtFiveDistinctOrdersWithoutActivatingIt() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryCandidateExtractor extractor = mock(ProjectMemoryCandidateExtractor.class);
        ProjectMemoryDocumentProvider provider = mock(ProjectMemoryDocumentProvider.class);
        ProjectMemoryManualCandidateFactory manualFactory = mock(ProjectMemoryManualCandidateFactory.class);
        ProjectMemoryCandidateEvidenceFactory evidenceFactory = mock(ProjectMemoryCandidateEvidenceFactory.class);
        ObjectMapper mapper = new ObjectMapper();
        AiProperties properties = new AiProperties();
        var proposal = proposal(mapper);
        when(provider.version("1.0.0")).thenReturn(Optional.of(memory(mapper)));
        when(extractor.extract(any(), any(), any())).thenReturn(List.of(proposal));
        when(repository.findByMemoryIdForUpdate(proposal.memoryId())).thenReturn(Optional.empty());
        when(repository.insert(any())).thenReturn(1);
        var evidence = new ProjectMemoryCandidateEvidenceWrite(
                "order-1", "parse-1", "AI_CONFIRMED", "cut twice",
                "{}", "{}", "{}", "{}", true, "admin", "hash");
        when(evidenceFactory.confirmed(any(), any())).thenReturn(evidence);
        when(repository.insertEvidence(anyString(), anyString(), any())).thenReturn(1);
        when(repository.countRecentOrders(anyString(), any())).thenReturn(5);
        when(repository.updateObservation(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any())).thenReturn(1);
        ProjectMemoryCandidateCaptureService service = new ProjectMemoryCandidateCaptureService(
                repository, extractor, manualFactory, evidenceFactory,
                provider, properties, mapper);

        service.capture(event());

        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(repository).updateObservation(anyString(), status.capture(),
                org.mockito.ArgumentMatchers.eq(5), any(LocalDateTime.class), any(LocalDateTime.class));
        assertThat(status.getValue()).isEqualTo("READY");
    }

    @Test
    void captureSubmission_repeatedExampleComparesExpectedConfiguration() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryCandidateExtractor extractor = mock(ProjectMemoryCandidateExtractor.class);
        ProjectMemoryDocumentProvider provider = mock(ProjectMemoryDocumentProvider.class);
        ProjectMemoryManualCandidateFactory manualFactory = mock(ProjectMemoryManualCandidateFactory.class);
        ProjectMemoryCandidateEvidenceFactory evidenceFactory = mock(ProjectMemoryCandidateEvidenceFactory.class);
        ObjectMapper mapper = new ObjectMapper();
        var expected = mapper.createObjectNode().put("mainStepType", 2);
        var document = mapper.createObjectNode();
        document.put("type", "EXAMPLE");
        document.put("scope", "PROCESS_ORDER");
        document.set("expected", expected);
        var proposal = new ProjectMemoryCandidateProposal(
                "example-1", "EXAMPLE", "PROCESS_ORDER",
                "FINAL_VALIDATED_CONFIGURATION", "客户要求", document, null);
        ProjectMemoryCandidateRow existing = new ProjectMemoryCandidateRow(
                "candidate-1", proposal.memoryId(), "EXAMPLE", document.toString(),
                "CANDIDATE", 1, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(90), null, null, null);
        when(provider.version("1.0.0")).thenReturn(Optional.of(memory(mapper)));
        when(manualFactory.create(any(), any())).thenReturn(Optional.of(proposal));
        when(repository.findByMemoryIdForUpdate(proposal.memoryId())).thenReturn(Optional.of(existing));
        when(evidenceFactory.manual(any(), any())).thenReturn(new ProjectMemoryCandidateEvidenceWrite(
                "order-1", null, "MANUAL_FINAL", "客户要求", "{}", "{}", "{}", "{}",
                true, "admin", "hash"));
        when(repository.insertEvidence(anyString(), anyString(), any())).thenReturn(1);
        when(repository.countRecentOrders(anyString(), any())).thenReturn(2);
        when(repository.updateObservation(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any())).thenReturn(1);
        ProjectMemoryCandidateCaptureService service = new ProjectMemoryCandidateCaptureService(
                repository, extractor, manualFactory, evidenceFactory,
                provider, new AiProperties(), mapper);
        ProjectMemorySubmissionLearningSnapshot snapshot = new ProjectMemorySubmissionLearningSnapshot(
                "order-1", "1.0.0", "客户要求", mapper.createObjectNode(), expected, "admin");

        service.captureSubmission(snapshot);

        verify(repository).insertEvidence(anyString(),
                org.mockito.ArgumentMatchers.eq("candidate-1"), any());
        verify(repository).updateObservation(org.mockito.ArgumentMatchers.eq("candidate-1"),
                org.mockito.ArgumentMatchers.eq("CANDIDATE"),
                org.mockito.ArgumentMatchers.eq(2), any(), any());
    }

    private ProjectMemoryCandidateConfirmedEvent event() {
        return new ProjectMemoryCandidateConfirmedEvent(
                "order-1", "parse-1", "1.0.0", "cut twice", "admin",
                null, List.of(), null, null);
    }

    private ProjectMemoryCandidateProposal proposal(ObjectMapper mapper) {
        var node = mapper.createObjectNode();
        node.put("scope", "SAW");
        node.put("intent", "SAW_CUTS");
        return new ProjectMemoryCandidateProposal(
                "term-candidate-1", "TERM", "SAW", "SAW_CUTS", "cut twice", node, null);
    }

    private ProjectMemorySnapshot memory(ObjectMapper mapper) {
        return new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:" + "a".repeat(64),
                mapper.createObjectNode(), Instant.now());
    }
}

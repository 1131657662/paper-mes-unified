package com.paper.mes.ai.memory.candidate;

import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectMemoryCandidateEvidenceReferenceBackfillTest {

    @Test
    void skipsBackfillWhenReferenceHmacKeyIsUnavailable() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryCandidateEvidenceReferenceBackfill service = service(repository, "");

        service.process();

        verifyNoInteractions(repository);
    }

    @Test
    void backfillsLegacyReferencesWhenReferenceHmacKeyIsConfigured() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        LegacyEvidenceReference reference = new LegacyEvidenceReference(
                "evidence-1", "candidate-1", "order-1", "parse-1");
        when(repository.findLegacyReferences(100)).thenReturn(List.of(reference));
        when(repository.backfillReference(eq("evidence-1"), eq(hash("order-1")),
                eq(hash("parse-1")))).thenReturn(1);

        ProjectMemoryCandidateEvidenceReferenceBackfill service = service(repository,
                "01234567890123456789012345678901");

        service.process();

        verify(repository).backfillReference("evidence-1", hash("order-1"), hash("parse-1"));
        verify(repository).refreshDistinctOrderCounts();
    }

    private ProjectMemoryCandidateEvidenceReferenceBackfill service(
            ProjectMemoryCandidateRepository repository, String key) {
        AiProperties properties = new AiProperties();
        properties.setMemoryReferenceHmacKey(key);
        return new ProjectMemoryCandidateEvidenceReferenceBackfill(repository,
                new ProcessAiMemoryReferenceHasher(properties));
    }

    private static String hash(String value) {
        AiProperties properties = new AiProperties();
        properties.setMemoryReferenceHmacKey("01234567890123456789012345678901");
        return new ProcessAiMemoryReferenceHasher(properties).hash(value);
    }
}

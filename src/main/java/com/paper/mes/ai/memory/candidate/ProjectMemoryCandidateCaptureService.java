package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectMemoryCandidateCaptureService {

    private static final Set<String> TERMINAL = Set.of("REJECTED", "EXPIRED", "CONFLICT");

    private final ProjectMemoryCandidateRepository repository;
    private final ProjectMemoryCandidateExtractor extractor;
    private final ProjectMemoryManualCandidateFactory manualFactory;
    private final ProjectMemoryCandidateEvidenceFactory evidenceFactory;
    private final ProjectMemoryDocumentProvider memoryProvider;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public void capture(ProjectMemoryCandidateConfirmedEvent event) {
        requireEvidenceReferencesReady();
        var memory = memoryProvider.version(event.projectMemoryVersion()).orElse(null);
        if (memory == null) throw new IllegalStateException("project-memory version unavailable");
        LocalDateTime now = LocalDateTime.now();
        repository.expireCandidates(now);
        extractor.extract(event.extraction(), event.acceptedFieldPaths(), memory)
                .forEach(proposal -> observe(proposal, evidenceFactory.confirmed(event, proposal), now));
    }

    @Transactional
    public void capture(ProjectMemoryCandidateLearningSnapshot event) {
        requireEvidenceReferencesReady();
        var memory = memoryProvider.version(event.projectMemoryVersion()).orElse(null);
        if (memory == null) throw new IllegalStateException("project-memory version unavailable");
        LocalDateTime now = LocalDateTime.now();
        repository.expireCandidates(now);
        if (event.extraction() == null) return;
        extractor.extract(event.extraction(), event.acceptedFieldPaths(), memory)
                .forEach(proposal -> observe(proposal,
                        evidenceFactory.confirmedSnapshot(event, proposal), now));
    }

    @Transactional
    public void captureSubmission(ProjectMemorySubmissionLearningSnapshot snapshot) {
        requireEvidenceReferencesReady();
        var memory = memoryProvider.version(snapshot.projectMemoryVersion()).orElse(null);
        if (memory == null) throw new IllegalStateException("project-memory version unavailable");
        LocalDateTime now = LocalDateTime.now();
        repository.expireCandidates(now);
        manualFactory.create(snapshot, memory).ifPresent(proposal ->
                observe(proposal, evidenceFactory.manual(snapshot, proposal), now));
    }

    @Transactional
    public void captureSubmittedOutbox(ProjectMemorySubmissionLearningOutboxSnapshot snapshot) {
        requireEvidenceReferencesReady();
        var memory = memoryProvider.version(snapshot.projectMemoryVersion()).orElse(null);
        if (memory == null) throw new IllegalStateException("project-memory version unavailable");
        LocalDateTime now = LocalDateTime.now();
        repository.expireCandidates(now);
        manualFactory.createFinalConfiguration(snapshot.finalConfiguration(), memory).ifPresent(proposal ->
                observe(proposal, evidenceFactory.manualSnapshot(snapshot, proposal), now));
    }

    private void observe(ProjectMemoryCandidateProposal proposal,
                         ProjectMemoryCandidateEvidenceWrite evidence,
                         LocalDateTime now) {
        ProjectMemoryCandidateRow row = repository.findByMemoryIdForUpdate(proposal.memoryId())
                .orElseGet(() -> insert(proposal, now));
        if (TERMINAL.contains(row.status())) return;
        if (!sameMeaning(row, proposal)) {
            requireUpdated(repository.updateObservation(
                    row.uuid(), "CONFLICT", 0, now, expiry(now)));
            return;
        }
        repository.insertEvidence(UUID.randomUUID().toString(), row.uuid(), evidence);
        int count = repository.countRecentOrders(row.uuid(),
                now.minusDays(properties.getMemoryCandidateWindowDays()));
        String status = "ACTIVE".equals(row.status()) ? "ACTIVE"
                : count >= properties.getMemoryCandidateMinOrders() ? "READY" : row.status();
        requireUpdated(repository.updateObservation(row.uuid(), status, count, now, expiry(now)));
    }

    private ProjectMemoryCandidateRow insert(ProjectMemoryCandidateProposal proposal,
                                             LocalDateTime now) {
        ProjectMemoryCandidateRow row = new ProjectMemoryCandidateRow(
                UUID.randomUUID().toString(), proposal.memoryId(), proposal.candidateType(),
                proposal.document().toString(), "CANDIDATE", 0, now, now, expiry(now),
                null, null, null);
        requireUpdated(repository.insert(row));
        return row;
    }

    private boolean sameMeaning(ProjectMemoryCandidateRow row,
                                ProjectMemoryCandidateProposal proposal) {
        try {
            var existing = objectMapper.readTree(row.candidateJson());
            if (!proposal.scope().equals(existing.path("scope").asText())) return false;
            String meaningField = "EXAMPLE".equals(proposal.candidateType())
                    ? "expected" : "intent";
            return existing.path(meaningField).equals(proposal.document().path(meaningField));
        } catch (Exception exception) {
            return false;
        }
    }

    private LocalDateTime expiry(LocalDateTime now) {
        return now.plusDays(properties.getMemoryCandidateTtlDays());
    }

    private void requireEvidenceReferencesReady() {
        if (!repository.evidenceReferencesReady()) {
            throw new IllegalStateException("memory evidence reference backfill is incomplete");
        }
    }

    private void requireUpdated(int count) {
        if (count != 1) throw new IllegalStateException("memory candidate update failed");
    }
}

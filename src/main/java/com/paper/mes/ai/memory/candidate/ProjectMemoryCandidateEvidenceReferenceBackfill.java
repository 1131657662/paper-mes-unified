package com.paper.mes.ai.memory.candidate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Converts legacy evidence references to HMAC values before clearing raw identifiers. */
@Slf4j
@Service
@RequiredArgsConstructor
class ProjectMemoryCandidateEvidenceReferenceBackfill {

    private static final int BATCH_SIZE = 100;

    private final ProjectMemoryCandidateRepository repository;
    private final ProcessAiMemoryReferenceHasher referenceHasher;
    private final AtomicBoolean unavailableWarningLogged = new AtomicBoolean();

    @Scheduled(initialDelayString = "${app.ai.memory.evidence-backfill-initial-delay-ms:30000}",
            fixedDelayString = "${app.ai.memory.evidence-backfill-ms:60000}")
    @Transactional
    public void process() {
        if (!referenceHasher.isConfigured()) {
            if (unavailableWarningLogged.compareAndSet(false, true)) {
                log.warn("AI memory evidence reference backfill disabled: HMAC key is not configured");
            }
            return;
        }
        List<LegacyEvidenceReference> references = repository.findLegacyReferences(BATCH_SIZE);
        for (LegacyEvidenceReference reference : references) {
            String orderHash = reference.orderUuid() == null
                    ? null : referenceHasher.hash(reference.orderUuid());
            String parseHash = reference.parseId() == null
                    ? null : referenceHasher.hash(reference.parseId());
            if (repository.backfillReference(reference.uuid(), orderHash, parseHash) != 1) {
                log.warn("AI memory evidence reference changed before backfill: evidenceId={}",
                        reference.uuid());
            }
        }
        if (!references.isEmpty()) repository.refreshDistinctOrderCounts();
    }
}

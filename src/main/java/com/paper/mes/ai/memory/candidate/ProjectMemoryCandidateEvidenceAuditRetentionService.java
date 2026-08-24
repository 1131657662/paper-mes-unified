package com.paper.mes.ai.memory.candidate;

import com.paper.mes.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Removes expired encrypted audit content while retaining its integrity hash. */
@Service
@RequiredArgsConstructor
class ProjectMemoryCandidateEvidenceAuditRetentionService {

    private final ProjectMemoryCandidateRepository repository;
    private final AiProperties properties;

    @Scheduled(fixedDelayString = "${app.ai.memory.evidence-audit-retention-ms:86400000}")
    @Transactional
    public void purgeExpiredCiphertext() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusDays(properties.getMemoryEvidenceAuditRetentionDays());
        repository.purgeAuditContextBefore(cutoff);
    }
}

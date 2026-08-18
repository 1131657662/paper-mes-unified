package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProcessAiPackagingCandidateRegistrar {

    private final ProcessAiPackagingCandidateRepository repository;

    void register(ProcessAiParseRecord parse, ProcessAiConfirmResponse response,
                  String createdBy) {
        for (ProcessAiPackagingCandidate candidate : response.packagingCandidates()) {
            repository.insert(new ProcessAiPackagingCandidateRow(
                    UUID.randomUUID().toString(), parse.orderUuid(), parse.conversationId(),
                    parse.parseId(), parse.parseRevision(), candidate.ownerRollRef(),
                    candidate.originalUuid(), "PENDING", createdBy, null, LocalDateTime.now()));
        }
    }
}

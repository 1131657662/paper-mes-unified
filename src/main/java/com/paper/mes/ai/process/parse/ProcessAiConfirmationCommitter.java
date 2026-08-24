package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.memory.candidate.ProjectMemoryCandidateConfirmedEvent;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessAiConfirmationCommitter {

    private final ProcessAiConfirmationWriter writer;
    private final ProcessAiConfirmationContextGuard contextGuard;
    private final ProcessAiConfirmationAuditRecorder auditRecorder;
    private final ProcessAiConfirmationCodec codec;
    private final ApplicationEventPublisher eventPublisher;

    ProcessAiConfirmResponse commit(ProcessAiConfirmationPreparation preparation,
                                    ProcessAiCompilationResult compilation) {
        ProcessAiConfirmResponse response = writer.confirm(new ProcessAiConfirmationWriteCommand(
                preparation.load(), compilation, preparation.userUuid(),
                preparation.customerRequirement()));
        auditRecorder.record(preparation, response);
        contextGuard.advanceConversation(
                response.conversationId(), response.expectedVersion(), response.nextVersion());
        eventPublisher.publishEvent(new ProjectMemoryCandidateConfirmedEvent(
                preparation.load().record().orderUuid(), response.parseId(),
                preparation.load().record().projectMemoryVersion(),
                preparation.redaction().sanitizedText(), preparation.userUuid(),
                preparation.load().extraction(), response.acceptedFieldPaths(),
                compilation, preparation.context().baseline(),
                codec.readCorrections(preparation.load().record()),
                preparation.load().acknowledgedDefaultIds(), response.previewHash()));
        return response;
    }
}

package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessAiConfirmationPreparationService {

    private final ProcessAiConfirmCandidateLoader candidateLoader;
    private final ProcessAiConfirmationContextGuard contextGuard;
    private final ProcessAiMessageService messageService;
    private final ProcessAiCustomerRequirementResolver requirementResolver;

    ProcessAiConfirmationPreparation prepare(String orderUuid, ProcessAiConfirmRequest request) {
        String userUuid = contextGuard.requireOwner(orderUuid, request.conversationId());
        ProcessAiConfirmationLoad load = candidateLoader.load(orderUuid, request);
        if (load.isReplay()) {
            return new ProcessAiConfirmationPreparation(userUuid, load, null, null, null);
        }
        ProcessAiOrderContext context = contextGuard.lockAndRead(
                orderUuid, request.conversationId(), request.expectedVersion(),
                load.record().memoryGeneration());
        var messages = messageService.restore(
                orderUuid, request.conversationId(), request.expectedVersion());
        String requirement = requirementResolver.resolve(context.remarkLong(), messages);
        String parseRequirement = messageService.restoreUserMessage(
                orderUuid, request.conversationId(), request.expectedVersion(),
                load.record().requestIdempotencyKey());
        return new ProcessAiConfirmationPreparation(
                userUuid, load, context, requirement,
                requirementResolver.redactionForConfirmation(parseRequirement, messages));
    }
}

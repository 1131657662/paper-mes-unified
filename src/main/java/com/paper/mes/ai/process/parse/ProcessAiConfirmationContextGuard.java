package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.processorder.service.DraftOrderVersionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessAiConfirmationContextGuard {

    private final ProcessAiConversationService conversationService;
    private final DraftOrderVersionGuard versionGuard;
    private final CloudDbContextReader contextReader;
    private final PermissionChecker permissionChecker;

    String requireOwner(String orderUuid, String conversationId) {
        permissionChecker.require(Permissions.ORDER_CREATE);
        permissionChecker.require(Permissions.AI_ASSIST);
        return conversationService.requireConfirmationOwner(orderUuid, conversationId);
    }

    ProcessAiOrderContext lockAndRead(String orderUuid, String conversationId,
                                       int expectedVersion, int memoryGeneration) {
        versionGuard.assertLockedExpected(orderUuid, expectedVersion);
        conversationService.lockForConfirmation(
                orderUuid, conversationId, expectedVersion, memoryGeneration);
        return contextReader.read(orderUuid, expectedVersion);
    }

    void advanceConversation(String conversationId, int expectedVersion, int nextVersion) {
        conversationService.advanceDraftVersion(conversationId, expectedVersion, nextVersion);
    }
}

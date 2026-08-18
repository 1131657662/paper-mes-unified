package com.paper.mes.ai.process.session;

import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessAiMessageAccessGuard {

    private final CloudDbContextReader contextReader;
    private final ProcessAiConversationRepository conversationRepository;

    ProcessAiConversationRow requireReadable(String orderUuid, String conversationId,
                                              int expectedVersion) {
        contextReader.read(orderUuid, expectedVersion);
        ProcessAiConversationRow conversation = conversationRepository.findByOrder(orderUuid)
                .filter(row -> row.conversationId().equals(conversationId))
                .orElseThrow(this::notFound);
        requireOwner(conversation);
        return conversation;
    }

    ProcessAiConversationRow requireWritable(String orderUuid, String conversationId,
                                              int expectedVersion) {
        contextReader.read(orderUuid, expectedVersion);
        ProcessAiConversationRow conversation = conversationRepository.findByOrderForUpdate(orderUuid)
                .filter(row -> row.conversationId().equals(conversationId))
                .orElseThrow(this::notFound);
        requireOwner(conversation);
        if (!"OPEN".equals(conversation.status()) && !"INTERRUPTED".equals(conversation.status())) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "AI_CONVERSATION_CLOSED", "该加工单AI会话已关闭");
        }
        return conversation;
    }

    private void requireOwner(ProcessAiConversationRow row) {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null || !row.userUuid().equals(user.getUuid())) {
            throw notFound();
        }
    }

    private BusinessException notFound() {
        return new BusinessException(ResultCode.NOT_FOUND,
                "AI_CONVERSATION_NOT_FOUND", "AI会话不存在");
    }
}

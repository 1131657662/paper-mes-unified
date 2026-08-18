package com.paper.mes.ai.process.session;

import lombok.RequiredArgsConstructor;
import com.paper.mes.ai.process.stream.ProcessAiSingleFlightRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessAiConversationCleanupWorker {

    private final ProcessAiConversationRepository conversationRepository;
    private final ProcessAiMessageRepository messageRepository;
    private final ProcessAiSingleFlightRegistry singleFlightRegistry;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void close(String orderUuid) {
        conversationRepository.findByOrderForUpdate(orderUuid).ifPresent(conversation -> {
            if (singleFlightRegistry.isConversationInFlight(conversation.conversationId())) return;
            messageRepository.deleteByConversation(conversation.conversationId());
            conversationRepository.close(conversation.conversationId());
        });
    }
}

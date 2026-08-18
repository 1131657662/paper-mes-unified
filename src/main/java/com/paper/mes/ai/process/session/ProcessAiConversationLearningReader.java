package com.paper.mes.ai.process.session;

import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProcessAiConversationLearningReader {

    private static final int MAX_REQUIREMENT_CHARS = 2_000;

    private final ProcessAiConversationRepository conversationRepository;
    private final ProcessAiMessageRepository messageRepository;
    private final AiMessageCipher messageCipher;

    @Transactional(readOnly = true)
    public Optional<ProcessAiConversationLearningContext> read(String orderUuid) {
        return conversationRepository.findByOrder(orderUuid).flatMap(conversation -> {
            LinkedHashSet<String> messages = new LinkedHashSet<>();
            for (ProcessAiMessageRow row : messageRepository.findByConversation(
                    conversation.conversationId(), conversation.memoryGeneration())) {
                if (!"USER".equals(row.role()) || !"FINAL".equals(row.messageStatus())) continue;
                String content = messageCipher.decrypt(new AiMessageCryptoContext(
                        row.conversationId(), row.sequenceNo(), row.role()),
                        row.contentCiphertext()).trim();
                if (!content.isBlank()) messages.add(content);
            }
            String requirement = String.join("\n", messages);
            if (requirement.isBlank()) return Optional.empty();
            if (requirement.length() > MAX_REQUIREMENT_CHARS) {
                requirement = requirement.substring(requirement.length() - MAX_REQUIREMENT_CHARS);
            }
            return Optional.of(new ProcessAiConversationLearningContext(
                    conversation.projectMemoryVersion(), requirement));
        });
    }
}

package com.paper.mes.ai.process.session;

import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import com.paper.mes.ai.process.session.crypto.AiStructuredResultCipher;
import com.paper.mes.ai.process.session.dto.AppendUserMessageCommand;
import com.paper.mes.ai.process.session.dto.CreateAssistantMessageCommand;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessAiMessageService {

    private static final int MAX_USER_MESSAGE_CHARS = 2_000;

    private final ProcessAiMessageAccessGuard accessGuard;
    private final ProcessAiMessageRepository messageRepository;
    private final AiMessageCipher cipher;
    private final AiStructuredResultCipher structuredResultCipher;

    @Transactional(readOnly = true)
    public List<ProcessAiMessageResponse> restore(String orderUuid, String conversationId,
                                                   int expectedVersion) {
        ProcessAiConversationRow conversation = accessGuard.requireReadable(
                orderUuid, conversationId, expectedVersion);
        return messageRepository.findByConversation(
                        conversationId, conversation.memoryGeneration()).stream()
                .map(this::decrypt)
                .toList();
    }

    @Transactional(readOnly = true)
    public String restoreUserMessage(String orderUuid, String conversationId,
                                     int expectedVersion, String idempotencyKey) {
        ProcessAiConversationRow conversation = accessGuard.requireReadable(
                orderUuid, conversationId, expectedVersion);
        ProcessAiMessageRow row = messageRepository.findByIdempotencyKey(
                        conversationId, conversation.memoryGeneration(), idempotencyKey)
                .filter(message -> "USER".equals(message.role()))
                .orElseThrow(() -> notFound("AI_MESSAGE_NOT_FOUND", "AI请求原话不存在"));
        return decrypt(row).content();
    }

    @Transactional(readOnly = true)
    public String restoreAssistantMessage(String orderUuid, String conversationId,
                                          int expectedVersion, int sequenceNo) {
        ProcessAiConversationRow conversation = accessGuard.requireReadable(
                orderUuid, conversationId, expectedVersion);
        return decrypt(requireAssistant(
                conversationId, conversation.memoryGeneration(), sequenceNo)).content();
    }

    @Transactional
    public ProcessAiMessageResponse appendUser(AppendUserMessageCommand command) {
        requireMessage(command.idempotencyKey(), command.content());
        ProcessAiConversationRow conversation = accessGuard.requireWritable(
                command.orderUuid(), command.conversationId(),
                command.expectedVersion());
        return messageRepository.findByIdempotencyKey(
                        command.conversationId(), conversation.memoryGeneration(),
                        command.idempotencyKey())
                .map(row -> requireSameRequest(row, command.content()))
                .orElseGet(() -> insertUser(conversation,
                        command.idempotencyKey(), command.content()));
    }

    @Transactional
    public ProcessAiMessageResponse createAssistant(CreateAssistantMessageCommand command) {
        ProcessAiConversationRow conversation = accessGuard.requireWritable(
                command.orderUuid(), command.conversationId(),
                command.expectedVersion());
        int sequence = messageRepository.nextSequence(command.conversationId());
        return insertAssistant(conversation, sequence);
    }

    @Transactional
    public ProcessAiMessageResponse updateAssistant(UpdateAssistantMessageCommand command) {
        ProcessAiConversationRow conversation = accessGuard.requireWritable(
                command.orderUuid(), command.conversationId(),
                command.expectedVersion());
        ProcessAiMessageRow current = requireAssistant(command.conversationId(),
                conversation.memoryGeneration(), command.sequenceNo());
        if (!"PARTIAL".equals(current.messageStatus())) {
            return requireSameTerminal(current, command);
        }
        ProcessAiMessageRow updated = encryptAssistant(current, command);
        if (messageRepository.updateContent(updated) != 1) {
            throw conflict("AI_MESSAGE_UPDATE_CONFLICT", "AI回复状态已变化");
        }
        return response(updated, command.content());
    }

    private ProcessAiMessageResponse requireSameTerminal(
            ProcessAiMessageRow current, UpdateAssistantMessageCommand command) {
        boolean sameTerminal = current.messageStatus().equals(command.status())
                && cipher.hash(command.content()).equals(current.contentHash());
        if (!sameTerminal) {
            throw conflict("AI_MESSAGE_UPDATE_CONFLICT", "AI回复已进入终态");
        }
        return decrypt(current);
    }

    private ProcessAiMessageResponse insertUser(ProcessAiConversationRow conversation,
                                                String key, String content) {
        String conversationId = conversation.conversationId();
        int sequence = messageRepository.nextSequence(conversationId);
        AiMessageCryptoContext context = new AiMessageCryptoContext(conversationId, sequence, "USER");
        ProcessAiMessageRow row = new ProcessAiMessageRow(
                UUID.randomUUID().toString(), conversationId, conversation.memoryGeneration(),
                sequence, "USER", "FINAL", key,
                cipher.encrypt(context, content), cipher.hash(content), null, LocalDateTime.now());
        messageRepository.insert(row);
        return response(row, content);
    }

    private ProcessAiMessageResponse insertAssistant(ProcessAiConversationRow conversation,
                                                     int sequence) {
        String conversationId = conversation.conversationId();
        AiMessageCryptoContext context = new AiMessageCryptoContext(conversationId, sequence, "ASSISTANT");
        String messageUuid = UUID.randomUUID().toString();
        ProcessAiMessageRow row = new ProcessAiMessageRow(
                messageUuid, conversationId, conversation.memoryGeneration(), sequence,
                "ASSISTANT", "PARTIAL",
                "assistant:" + messageUuid,
                cipher.encrypt(context, ""), cipher.hash(""), null, LocalDateTime.now());
        messageRepository.insert(row);
        return response(row, "");
    }

    private ProcessAiMessageRow requireAssistant(String conversationId, int generation,
                                                 int sequence) {
        return messageRepository.findBySequence(conversationId, generation, sequence)
                .filter(row -> "ASSISTANT".equals(row.role()))
                .orElseThrow(() -> notFound("AI_MESSAGE_NOT_FOUND", "AI回复不存在"));
    }

    private ProcessAiMessageRow encryptAssistant(ProcessAiMessageRow current,
                                                   UpdateAssistantMessageCommand command) {
        if (!java.util.Set.of("PARTIAL", "FINAL", "FAILED").contains(command.status())
                || command.content() == null || command.content().length() > 16_000) {
            throw badRequest("AI_ASSISTANT_MESSAGE_INVALID", "AI回复状态或长度无效");
        }
        AiMessageCryptoContext context = new AiMessageCryptoContext(
                current.conversationId(), current.sequenceNo(), current.role());
        String storedResult = structuredResultCipher.encrypt(context, command.structuredResult());
        return new ProcessAiMessageRow(current.uuid(), current.conversationId(),
                current.memoryGeneration(), current.sequenceNo(), current.role(), command.status(),
                null, cipher.encrypt(context, command.content()),
                cipher.hash(command.content()), storedResult, current.createdAt());
    }

    private ProcessAiMessageResponse requireSameRequest(ProcessAiMessageRow row, String content) {
        if (!"USER".equals(row.role()) || !cipher.hash(content).equals(row.contentHash())) {
            throw conflict("AI_MESSAGE_IDEMPOTENCY_CONFLICT", "该请求标识已用于不同消息");
        }
        return decrypt(row);
    }

    private ProcessAiMessageResponse decrypt(ProcessAiMessageRow row) {
        AiMessageCryptoContext context = new AiMessageCryptoContext(
                row.conversationId(), row.sequenceNo(), row.role());
        return response(row, cipher.decrypt(context, row.contentCiphertext()),
                structuredResultCipher.decrypt(context, row.structuredResult()));
    }

    private ProcessAiMessageResponse response(ProcessAiMessageRow row, String content) {
        AiMessageCryptoContext context = new AiMessageCryptoContext(
                row.conversationId(), row.sequenceNo(), row.role());
        return response(row, content,
                structuredResultCipher.decrypt(context, row.structuredResult()));
    }

    private ProcessAiMessageResponse response(ProcessAiMessageRow row, String content,
                                              String structuredResult) {
        return new ProcessAiMessageResponse(
                row.sequenceNo(), row.role(), row.messageStatus(), content,
                structuredResult, row.createdAt());
    }

    private void requireMessage(String key, String content) {
        if (key == null || key.isBlank() || key.length() > 80) {
            throw badRequest("AI_IDEMPOTENCY_KEY_INVALID", "请求标识不能为空且不能超过80字符");
        }
        if (content == null || content.isBlank() || content.length() > MAX_USER_MESSAGE_CHARS) {
            throw badRequest("AI_MESSAGE_INVALID", "消息不能为空且不能超过2000字符");
        }
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(ResultCode.NOT_FOUND, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}

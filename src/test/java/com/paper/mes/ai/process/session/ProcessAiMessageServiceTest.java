package com.paper.mes.ai.process.session;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import com.paper.mes.ai.process.session.crypto.AiStructuredResultCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.session.dto.AppendUserMessageCommand;
import com.paper.mes.ai.process.session.dto.CreateAssistantMessageCommand;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiMessageServiceTest {

    private final CloudDbContextReader contextReader = mock(CloudDbContextReader.class);
    private final ProcessAiConversationRepository conversationRepository = mock(ProcessAiConversationRepository.class);
    private final ProcessAiMessageRepository messageRepository = mock(ProcessAiMessageRepository.class);
    private AiMessageCipher cipher;
    private AiStructuredResultCipher structuredCipher;
    private ProcessAiMessageService service;
    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setMessageEncryptionKey(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        cipher = new AiMessageCipher(properties);
        structuredCipher = new AiStructuredResultCipher(cipher, new ObjectMapper());
        service = new ProcessAiMessageService(new ProcessAiMessageAccessGuard(
                contextReader, conversationRepository), messageRepository, cipher,
                structuredCipher);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void appendUserStoresOnlyCiphertextAndReturnsPlaintext() {
        when(conversationRepository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findByIdempotencyKey("conversation-1", 1, "request-1"))
                .thenReturn(Optional.empty());
        when(messageRepository.nextSequence("conversation-1")).thenReturn(1);

        var response = service.appendUser(command("request-1", "客户说切2刀"));

        ArgumentCaptor<ProcessAiMessageRow> captor = ArgumentCaptor.forClass(ProcessAiMessageRow.class);
        verify(messageRepository).insert(captor.capture());
        assertThat(captor.getValue().contentCiphertext()).doesNotContain("客户说切2刀");
        assertThat(captor.getValue().contentHash()).isEqualTo(cipher.hash("客户说切2刀"));
        assertThat(response.content()).isEqualTo("客户说切2刀");
    }

    @Test
    void appendUserReturnsTheOriginalMessageForAnIdempotentReplay() {
        ProcessAiMessageRow existing = encryptedRow("request-1", "客户原话");
        when(conversationRepository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findByIdempotencyKey("conversation-1", 1, "request-1"))
                .thenReturn(Optional.of(existing));

        var response = service.appendUser(command("request-1", "客户原话"));

        assertThat(response.content()).isEqualTo("客户原话");
        verify(messageRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void appendUserRejectsAnIdempotencyKeyReusedForDifferentContent() {
        when(conversationRepository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findByIdempotencyKey("conversation-1", 1, "request-1"))
                .thenReturn(Optional.of(encryptedRow("request-1", "第一条")));

        BusinessException error = catchThrowableOfType(
                () -> service.appendUser(command("request-1", "另一条")), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(409);
        assertThat(error.getErrorCode()).isEqualTo("AI_MESSAGE_IDEMPOTENCY_CONFLICT");
    }

    @Test
    void restoreReturnsMessagesInRepositoryOrder() {
        when(conversationRepository.findByOrder("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findByConversation("conversation-1", 1))
                .thenReturn(List.of(encryptedRow("request-1", "客户原话")));

        var messages = service.restore("order-1", "conversation-1", 7);

        assertThat(messages).extracting(message -> message.content())
                .containsExactly("客户原话");
    }

    @Test
    void restoreHidesConversationExistenceFromAnotherUser() {
        when(conversationRepository.findByOrder("order-1"))
                .thenReturn(Optional.of(conversation("user-2", "OPEN")));

        BusinessException error = catchThrowableOfType(
                () -> service.restore("order-1", "conversation-1", 7), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(404);
        assertThat(error.getErrorCode()).isEqualTo("AI_CONVERSATION_NOT_FOUND");
        verify(messageRepository, never()).findByConversation("conversation-1", 1);
    }

    @Test
    void createAssistantStoresAnEncryptedPartialMessage() {
        when(conversationRepository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.nextSequence("conversation-1")).thenReturn(2);

        var response = service.createAssistant(
                new CreateAssistantMessageCommand("order-1", "conversation-1", 7));

        ArgumentCaptor<ProcessAiMessageRow> captor = ArgumentCaptor.forClass(ProcessAiMessageRow.class);
        verify(messageRepository).insert(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo("ASSISTANT");
        assertThat(captor.getValue().messageStatus()).isEqualTo("PARTIAL");
        assertThat(captor.getValue().idempotencyKey())
                .isEqualTo("assistant:" + captor.getValue().uuid());
        assertThat(response.sequenceNo()).isEqualTo(2);
    }

    @Test
    void updateAssistantReencryptsTheCompletedContent() {
        when(conversationRepository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findBySequence("conversation-1", 1, 2))
                .thenReturn(Optional.of(encryptedAssistantRow()));
        when(messageRepository.updateContent(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        var response = service.updateAssistant(new UpdateAssistantMessageCommand(
                "order-1", "conversation-1", 7, 2, "解析完成", "FINAL",
                "{\"unitPrice\":20}"));

        ArgumentCaptor<ProcessAiMessageRow> captor = ArgumentCaptor.forClass(ProcessAiMessageRow.class);
        verify(messageRepository).updateContent(captor.capture());
        assertThat(response.status()).isEqualTo("FINAL");
        assertThat(response.content()).isEqualTo("解析完成");
        assertThat(response.structuredResult()).isEqualTo("{\"unitPrice\":20}");
        assertThat(captor.getValue().structuredResult()).doesNotContain("unitPrice");
        assertThat(structuredCipher.decrypt(
                new AiMessageCryptoContext("conversation-1", 2, "ASSISTANT"),
                captor.getValue().structuredResult())).isEqualTo("{\"unitPrice\":20}");
    }

    @Test
    void updateAssistantDoesNotOverwriteAFinalMessageWithFailure() {
        when(conversationRepository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findBySequence("conversation-1", 1, 2))
                .thenReturn(Optional.of(encryptedAssistantRow("FINAL", "解析完成")));

        BusinessException error = catchThrowableOfType(() -> service.updateAssistant(
                new UpdateAssistantMessageCommand("order-1", "conversation-1", 7, 2,
                        "解析中断", "FAILED", null)), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_MESSAGE_UPDATE_CONFLICT");
        verify(messageRepository, never()).updateContent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void restoreUserMessageUsesTheExactIdempotencyKey() {
        when(conversationRepository.findByOrder("order-1"))
                .thenReturn(Optional.of(conversation("user-1", "OPEN")));
        when(messageRepository.findByIdempotencyKey("conversation-1", 1, "request-2"))
                .thenReturn(Optional.of(encryptedRow("request-2", "包膜加20元每件")));

        String content = service.restoreUserMessage(
                "order-1", "conversation-1", 7, "request-2");

        assertThat(content).isEqualTo("包膜加20元每件");
    }

    private AppendUserMessageCommand command(String key, String content) {
        return new AppendUserMessageCommand("order-1", "conversation-1", 7, key, content);
    }

    private ProcessAiConversationRow conversation(String userUuid, String status) {
        return new ProcessAiConversationRow("row-1", "conversation-1", "order-1",
                userUuid, 3, 7, "1.0.0", 1, status);
    }

    private ProcessAiMessageRow encryptedRow(String key, String content) {
        var context = new AiMessageCryptoContext("conversation-1", 1, "USER");
        return new ProcessAiMessageRow("message-1", "conversation-1", 1, 1, "USER", "FINAL",
                key, cipher.encrypt(context, content), cipher.hash(content), null, LocalDateTime.now());
    }

    private ProcessAiMessageRow encryptedAssistantRow() {
        return encryptedAssistantRow("PARTIAL", "");
    }

    private ProcessAiMessageRow encryptedAssistantRow(String status, String content) {
        var context = new AiMessageCryptoContext("conversation-1", 2, "ASSISTANT");
        return new ProcessAiMessageRow("message-2", "conversation-1", 1, 2, "ASSISTANT", status,
                "assistant:2", cipher.encrypt(context, content), cipher.hash(content), null, LocalDateTime.now());
    }
}

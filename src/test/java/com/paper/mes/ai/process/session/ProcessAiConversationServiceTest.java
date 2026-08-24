package com.paper.mes.ai.process.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.session.dto.ProcessAiSessionRequest;
import com.paper.mes.ai.process.session.dto.ReserveProcessAiParseCommand;
import com.paper.mes.ai.process.stream.ProcessAiSingleFlightRegistry;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiConversationServiceTest {

    private final CloudDbContextReader contextReader = mock(CloudDbContextReader.class);
    private final ProjectMemoryDocumentProvider memoryProvider = mock(ProjectMemoryDocumentProvider.class);
    private final ProcessAiConversationRepository repository = mock(ProcessAiConversationRepository.class);
    private final ProcessAiSingleFlightRegistry singleFlightRegistry =
            mock(ProcessAiSingleFlightRegistry.class);
    private ProcessAiConversationService service;

    @BeforeEach
    void setUp() {
        service = new ProcessAiConversationService(
                contextReader, memoryProvider, repository, singleFlightRegistry);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
        when(memoryProvider.current()).thenReturn(Optional.of(memory()));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void openCreatesAnOrderScopedConversationOnFirstUse() {
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.empty());

        var response = service.open("order-1", request(7, 3));

        assertThat(response.resumed()).isFalse();
        assertThat(response.currentStep()).isEqualTo(3);
        assertThat(response.draftVersion()).isEqualTo(7);
        assertThat(response.projectMemoryVersion()).isEqualTo("1.0.0");
        verify(repository).insert(org.mockito.ArgumentMatchers.argThat(row ->
                "order-1".equals(row.orderUuid()) && "user-1".equals(row.userUuid())));
    }

    @Test
    void openResumesTheSameConversationForItsOwner() {
        ProcessAiConversationRow row = row("OPEN", "user-1", 7);
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.of(row));

        var response = service.open("order-1", request(7, 4));

        assertThat(response.resumed()).isTrue();
        assertThat(response.conversationId()).isEqualTo("conversation-1");
        assertThat(response.currentStep()).isEqualTo(4);
        verify(repository).reopen("conversation-1", 4);
    }

    @Test
    void openRejectsAConversationOwnedByAnotherUser() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("OPEN", "user-2", 7)));

        BusinessException error = catchThrowableOfType(
                () -> service.open("order-1", request(7, 3)), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(403);
        assertThat(error.getErrorCode()).isEqualTo("AI_CONVERSATION_FORBIDDEN");
        verify(repository, never()).reopen("conversation-1", 3);
    }

    @Test
    void openSynchronizesAConversationAfterOrdinaryDraftWrites() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("OPEN", "user-1", 6)));
        when(repository.advanceDraftVersion("conversation-1", 6, 7)).thenReturn(1);

        var response = service.open("order-1", request(7, 3));

        assertThat(response.draftVersion()).isEqualTo(7);
        verify(repository).advanceDraftVersion("conversation-1", 6, 7);
    }

    @Test
    void openRejectsAClientVersionOlderThanTheConversation() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("OPEN", "user-1", 8)));

        BusinessException error = catchThrowableOfType(
                () -> service.open("order-1", request(7, 3)), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CONVERSATION_VERSION_CONFLICT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLOSED", "EXPIRED"})
    void openRejectsAConversationThatCannotBeResumed(String status) {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row(status, "user-1", 7)));

        BusinessException error = catchThrowableOfType(
                () -> service.open("order-1", request(7, 3)), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(409);
        assertThat(error.getErrorCode()).isEqualTo("AI_CONVERSATION_CLOSED");
    }

    @Test
    void openFailsClosedForANewConversationWhenProjectMemoryIsUnavailable() {
        when(memoryProvider.current()).thenReturn(Optional.empty());
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.empty());

        BusinessException error = catchThrowableOfType(
                () -> service.open("order-1", request(7, 3)), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(503);
        assertThat(error.getErrorCode()).isEqualTo("AI_MEMORY_UNAVAILABLE");
        verify(repository).findByOrderForUpdate("order-1");
        verify(repository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void openResumesAnExistingConversationWhenCurrentMemoryIsUnavailable() {
        when(memoryProvider.current()).thenReturn(Optional.empty());
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("OPEN", "user-1", 7)));

        var response = service.open("order-1", request(7, 4));

        assertThat(response.resumed()).isTrue();
        assertThat(response.projectMemoryVersion()).isEqualTo("1.0.0");
        assertThat(response.latestProjectMemoryVersion()).isNull();
        assertThat(response.memoryRefreshAvailable()).isFalse();
    }

    @Test
    void reserveParseUsesTheConversationMemoryVersionAndNextRevision() {
        ProcessAiConversationRow row = row("INTERRUPTED", "user-1", 7);
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.of(row));
        when(repository.reserveNextRevision("conversation-1")).thenReturn(3);

        var reservation = service.reserveParse(
                new ReserveProcessAiParseCommand("order-1", "conversation-1", 7));

        assertThat(reservation.parseRevision()).isEqualTo(3);
        assertThat(reservation.projectMemoryVersion()).isEqualTo("1.0.0");
    }

    @Test
    void reserveParseSynchronizesAfterAPlanWasSavedWhileTheDrawerStayedOpen() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("OPEN", "user-1", 7)));
        when(repository.advanceDraftVersion("conversation-1", 7, 8)).thenReturn(1);
        when(repository.reserveNextRevision("conversation-1")).thenReturn(4);

        var reservation = service.reserveParse(
                new ReserveProcessAiParseCommand("order-1", "conversation-1", 8));

        assertThat(reservation.parseRevision()).isEqualTo(4);
        verify(repository).advanceDraftVersion("conversation-1", 7, 8);
    }

    @Test
    void reserveParseRejectsWhenTheRevisionUpdateDidNotAffectTheConversation() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("OPEN", "user-1", 7)));
        when(repository.reserveNextRevision("conversation-1")).thenReturn(0);

        BusinessException error = catchThrowableOfType(() -> service.reserveParse(
                new ReserveProcessAiParseCommand("order-1", "conversation-1", 7)),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CONVERSATION_REVISION_CONFLICT");
    }

    @Test
    void reserveParseRejectsTheNinthClarificationRound() {
        ProcessAiConversationRow row = new ProcessAiConversationRow(
                "row-1", "conversation-1", "order-1", "user-1", 3, 7,
                "1.0.0", 1, "OPEN", 8);
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.of(row));

        BusinessException error = catchThrowableOfType(() -> service.reserveParse(
                new ReserveProcessAiParseCommand("order-1", "conversation-1", 7, "CLARIFY")),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_LIMIT_REACHED");
        verify(repository, never()).reserveNextRevision("conversation-1", "CLARIFY");
    }

    @Test
    void refreshMemoryStartsANewGenerationWithoutDeletingHistory() {
        ProcessAiConversationRow old = new ProcessAiConversationRow(
                "row-1", "conversation-1", "order-1", "user-1",
                3, 7, "0.9.0", 2, "OPEN");
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.of(old));
        when(repository.refreshMemory("conversation-1", 2, "1.0.0")).thenReturn(1);

        var response = service.refreshMemory("order-1", "conversation-1", 7);

        assertThat(response.projectMemoryVersion()).isEqualTo("1.0.0");
        assertThat(response.memoryGeneration()).isEqualTo(3);
        assertThat(response.memoryRefreshAvailable()).isFalse();
        verify(repository).refreshMemory("conversation-1", 2, "1.0.0");
    }

    @Test
    void refreshMemoryRejectsWhileAParseIsInFlight() {
        ProcessAiConversationRow old = new ProcessAiConversationRow(
                "row-1", "conversation-1", "order-1", "user-1",
                3, 7, "0.9.0", 2, "OPEN");
        when(repository.findByOrderForUpdate("order-1")).thenReturn(Optional.of(old));
        when(singleFlightRegistry.isConversationInFlight("conversation-1")).thenReturn(true);

        BusinessException error = catchThrowableOfType(
                () -> service.refreshMemory("order-1", "conversation-1", 7),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_IN_PROGRESS");
        verify(repository, never()).refreshMemory(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private ProcessAiSessionRequest request(int version, int step) {
        return new ProcessAiSessionRequest(version, step);
    }

    private ProcessAiConversationRow row(String status, String userUuid, int version) {
        return new ProcessAiConversationRow("row-1", "conversation-1", "order-1",
                userUuid, 3, version, "1.0.0", 1, status);
    }

    private ProjectMemorySnapshot memory() {
        return new ProjectMemorySnapshot("1.0.0", "1.0", "sha256:test",
                new ObjectMapper().createObjectNode(), Instant.parse("2026-08-16T00:00:00Z"));
    }
}

package com.paper.mes.ai.process.session;

import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.stream.ProcessAiSingleFlightRegistry;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiConversationConfirmationTest {

    private final ProcessAiConversationRepository repository =
            mock(ProcessAiConversationRepository.class);
    private ProcessAiConversationService service;

    @BeforeEach
    void setUp() {
        service = new ProcessAiConversationService(
                mock(CloudDbContextReader.class),
                mock(ProjectMemoryDocumentProvider.class), repository,
                mock(ProcessAiSingleFlightRegistry.class));
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void requireConfirmationOwnerAllowsOnlyTheConversationOwner() {
        when(repository.findByOrder("order-1")).thenReturn(Optional.of(row("user-1", 7)));

        assertThat(service.requireConfirmationOwner("order-1", "conversation-1"))
                .isEqualTo("user-1");
    }

    @Test
    void lockForConfirmationRejectsAStaleConversationVersion() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("user-1", 6)));

        BusinessException error = catchThrowableOfType(
                () -> service.lockForConfirmation("order-1", "conversation-1", 7, 1),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CONVERSATION_VERSION_CONFLICT");
    }

    @Test
    void advanceDraftVersionMovesTheOpenConversationToTheNextOrderVersion() {
        when(repository.advanceDraftVersion("conversation-1", 7, 8)).thenReturn(1);

        service.advanceDraftVersion("conversation-1", 7, 8);

        verify(repository).advanceDraftVersion("conversation-1", 7, 8);
    }

    @Test
    void lockForConfirmationRejectsAParseFromAnOlderMemoryGeneration() {
        when(repository.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(row("user-1", 7)));

        BusinessException error = catchThrowableOfType(
                () -> service.lockForConfirmation("order-1", "conversation-1", 7, 0),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_MEMORY_GENERATION_CONFLICT");
    }

    private ProcessAiConversationRow row(String userUuid, int version) {
        return new ProcessAiConversationRow(
                "row-1", "conversation-1", "order-1", userUuid,
                3, version, "1.0.0", 1, "OPEN");
    }
}

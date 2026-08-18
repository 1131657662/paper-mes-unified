package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiPackagingCandidateQueryServiceTest {

    private final CloudDbContextReader contextReader = mock(CloudDbContextReader.class);
    private final PermissionChecker permissionChecker = mock(PermissionChecker.class);
    private final ProcessAiPackagingCandidateRepository repository =
            mock(ProcessAiPackagingCandidateRepository.class);
    private final ProcessAiConfirmationCodec codec = mock(ProcessAiConfirmationCodec.class);
    private final ProcessAiPackagingCandidateQueryService service =
            new ProcessAiPackagingCandidateQueryService(
                    contextReader, permissionChecker, repository, codec);

    @BeforeEach
    void setUpUser() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
    }

    @AfterEach
    void clearUser() {
        AuthContextHolder.clear();
    }

    @Test
    void pendingScopesCandidatesToTheCurrentUser() {
        when(repository.findPending("order-1", "user-1")).thenReturn(List.of());

        assertThat(service.pending("order-1", 7)).isEmpty();

        verify(contextReader).read("order-1", 7);
        verify(repository).findPending("order-1", "user-1");
    }

    @Test
    void pendingRestoresCandidateFromTheEncryptedConfirmation() {
        ProcessAiPackagingCandidate candidate = candidate("R1", "roll-1");
        ProcessAiPackagingCandidateRow row = row("R1", "roll-1");
        when(repository.findPending("order-1", "user-1")).thenReturn(List.of(row));
        when(codec.readResponse("encrypted-confirmation", "conversation-1", 3))
                .thenReturn(response(candidate));

        var pending = service.pending("order-1", 7);

        assertThat(pending).singleElement().satisfies(value -> {
            assertThat(value.parseId()).isEqualTo("parse-1");
            assertThat(value.candidate()).isEqualTo(candidate);
        });
        verify(codec).readResponse("encrypted-confirmation", "conversation-1", 3);
    }

    @Test
    void pendingRejectsConfirmationWithoutTheRegisteredCandidate() {
        ProcessAiPackagingCandidateRow row = row("R1", "roll-1");
        when(repository.findPending("order-1", "user-1")).thenReturn(List.of(row));
        when(codec.readResponse("encrypted-confirmation", "conversation-1", 3))
                .thenReturn(response(candidate("R2", "roll-2")));

        BusinessException error = catchThrowableOfType(
                () -> service.pending("order-1", 7), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PACKAGING_CANDIDATE_INVALID");
    }

    private ProcessAiPackagingCandidateRow row(String ownerRollRef, String originalUuid) {
        return new ProcessAiPackagingCandidateRow(
                "candidate-1", "order-1", "conversation-1", "parse-1", 3,
                ownerRollRef, originalUuid, "PENDING", "user-1",
                "encrypted-confirmation", LocalDateTime.of(2026, 8, 17, 12, 0));
    }

    private ProcessAiConfirmResponse response(ProcessAiPackagingCandidate candidate) {
        return new ProcessAiConfirmResponse(
                "conversation-1", "parse-1", 3, 7, 8, "CONFIRMED",
                List.of(), Map.of(), List.of(candidate), List.of(), null, "hash");
    }

    private ProcessAiPackagingCandidate candidate(String ownerRollRef, String originalUuid) {
        return new ProcessAiPackagingCandidate(
                ownerRollRef, originalUuid, List.of(originalUuid), 4, "FILM_WRAP",
                "包膜", "PIECE", BigDecimal.ONE, 2, BigDecimal.TEN, null,
                "AI 候选，保存前人工确认");
    }
}

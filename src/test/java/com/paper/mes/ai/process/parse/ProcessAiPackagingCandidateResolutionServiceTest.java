package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiPackagingCandidateResolutionServiceTest {

    private final CloudDbContextReader contextReader = mock(CloudDbContextReader.class);
    private final PermissionChecker permissionChecker = mock(PermissionChecker.class);
    private final ProcessAiPackagingCandidateRepository repository =
            mock(ProcessAiPackagingCandidateRepository.class);
    private final ProcessAiPackagingCandidateResolutionService service =
            new ProcessAiPackagingCandidateResolutionService(
                    contextReader, permissionChecker, repository);

    @BeforeEach
    void setUpUser() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
    }

    @AfterEach
    void clearUser() {
        AuthContextHolder.clear();
    }

    @Test
    void markSavedResolvesTheCandidateLinkedToTheSavedServiceStep() {
        ProcessStepDTO step = new ProcessStepDTO();
        step.setAiParseId("parse-1");
        step.setAiOwnerRollRef("R1");
        when(repository.resolve("order-1", "parse-1", "R1", "user-1",
                "SAVED", "user-1")).thenReturn(1);

        service.markSaved("order-1", List.of(step));

        verify(repository).resolve("order-1", "parse-1", "R1", "user-1",
                "SAVED", "user-1");
    }

    @Test
    void dismissIsIdempotentWhenTheCandidateWasAlreadyDismissed() {
        when(repository.resolve("order-1", "parse-1", "R1", "user-1",
                "DISMISSED", "user-1")).thenReturn(0);
        when(repository.findStatus("order-1", "parse-1", "R1", "user-1"))
                .thenReturn(Optional.of("DISMISSED"));

        service.dismiss("order-1", "parse-1", "R1", 7);

        verify(contextReader).read("order-1", 7);
    }

    @Test
    void markSavedRejectsAnAlreadyDismissedCandidate() {
        ProcessStepDTO step = new ProcessStepDTO();
        step.setAiParseId("parse-1");
        step.setAiOwnerRollRef("R1");
        when(repository.resolve("order-1", "parse-1", "R1", "user-1",
                "SAVED", "user-1")).thenReturn(0);
        when(repository.findStatus("order-1", "parse-1", "R1", "user-1"))
                .thenReturn(Optional.of("DISMISSED"));

        BusinessException error = catchThrowableOfType(
                () -> service.markSaved("order-1", List.of(step)), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PACKAGING_CANDIDATE_RESOLVED");
    }
}

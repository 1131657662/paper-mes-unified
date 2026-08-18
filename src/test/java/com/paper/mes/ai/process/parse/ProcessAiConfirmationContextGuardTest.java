package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.RoleCodes;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.service.DraftOrderVersionGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiConfirmationContextGuardTest {

    private final ProcessAiConversationService conversationService =
            mock(ProcessAiConversationService.class);
    private final ProcessAiConfirmationContextGuard guard = new ProcessAiConfirmationContextGuard(
            conversationService, mock(DraftOrderVersionGuard.class),
            mock(CloudDbContextReader.class), new PermissionChecker());

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void requireOwnerRequiresBothOrderCreateAndAiAssistPermissions() {
        authenticate(RoleCodes.VIEWER);

        BusinessException error = catchThrowableOfType(
                () -> guard.requireOwner("order-1", "conversation-1"),
                BusinessException.class);

        assertThat(error.getCode()).isEqualTo(403);
        verify(conversationService, never())
                .requireConfirmationOwner("order-1", "conversation-1");
    }

    @Test
    void requireOwnerContinuesToConversationOwnershipForAnOrderClerk() {
        authenticate(RoleCodes.ORDER_CLERK);
        when(conversationService.requireConfirmationOwner("order-1", "conversation-1"))
                .thenReturn("user-1");

        assertThat(guard.requireOwner("order-1", "conversation-1")).isEqualTo("user-1");
    }

    private void authenticate(String roleCode) {
        AuthContextHolder.setCurrentUser(CurrentUser.builder()
                .uuid("user-1").roleCode(roleCode).build());
    }
}

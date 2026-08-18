package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProcessAiSessionControllerContractTest {

    @Test
    void controllerUsesTheOrderScopedProcessParsePath() {
        RequestMapping mapping = ProcessAiSessionController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.value())
                .containsExactly("/api/process-orders/{orderUuid}/ai/process-parse");
    }

    @Test
    void sessionEndpointUsesPostAndRequiresOrderCreatePermission() {
        var method = Arrays.stream(ProcessAiSessionController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("open"))
                .findFirst()
                .orElseThrow();
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        RequirePermission permission = ProcessAiSessionController.class
                .getAnnotation(RequirePermission.class);

        assertThat(mapping.value()).containsExactly("/session");
        assertThat(permission.value()).containsExactly(Permissions.ORDER_CREATE);
    }

    @Test
    void controllerCanBeConstructedWithTheSessionServiceBoundary() {
        ProcessAiConversationService service = mock(ProcessAiConversationService.class);

        assertThat(new ProcessAiSessionController(service)).isNotNull();
    }
}

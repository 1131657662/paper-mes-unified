package com.paper.mes.ai.process.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiParseControllerContractTest {

    @Test
    void confirmUsesTheOrderScopedPostEndpointAndOrderCreateRoutePermission() {
        var method = Arrays.stream(ProcessAiParseController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("confirm"))
                .findFirst()
                .orElseThrow();
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        RequirePermission permission = ProcessAiParseController.class
                .getAnnotation(RequirePermission.class);

        assertThat(mapping.value()).containsExactly("/confirm");
        assertThat(permission.value()).containsExactly(Permissions.ORDER_CREATE);
        assertThat(method.getParameterAnnotations()[1])
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().equals("Valid"));
    }
}

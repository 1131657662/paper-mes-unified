package com.paper.mes.ai.process.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderSettingsControllerContractTest {

    @Test
    void settingsEndpointIsRestrictedToSystemConfigurationAdministrators() {
        RequestMapping mapping = AiProviderSettingsController.class
                .getAnnotation(RequestMapping.class);
        RequirePermission permission = AiProviderSettingsController.class
                .getAnnotation(RequirePermission.class);

        assertThat(mapping.value()).containsExactly("/api/ai/provider-settings/{provider}");
        assertThat(permission.value()).containsExactly(Permissions.SYSTEM_CONFIG);
    }
}

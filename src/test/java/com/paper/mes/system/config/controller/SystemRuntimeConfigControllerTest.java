package com.paper.mes.system.config.controller;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.service.SystemConfigService;
import com.paper.mes.system.config.service.SystemDictService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemRuntimeConfigControllerTest {

    private final SystemDictService dictService = mock(SystemDictService.class);
    private final SystemConfigService configService = mock(SystemConfigService.class);
    private final SystemRuntimeConfigController controller =
            new SystemRuntimeConfigController(dictService, configService);

    @Test
    void configs_withPublicKey_queriesConfiguration() {
        when(configService.enabledByKeys(List.of("ui.defaultPageSize"))).thenReturn(List.of());

        controller.configs("ui.defaultPageSize");

        verify(configService).enabledByKeys(List.of("ui.defaultPageSize"));
    }

    @Test
    void configs_withProcessCreateKeys_queriesConfiguration() {
        List<String> keys = List.of("process.autoFinishConfig", "process.spareRollNoCount");
        when(configService.enabledByKeys(keys)).thenReturn(List.of());

        controller.configs(String.join(",", keys));

        verify(configService).enabledByKeys(keys);
    }

    @Test
    void configs_withPrivateKey_rejectsRequest() {
        assertThrows(BusinessException.class, () -> controller.configs("integration.secret"));
    }
}

package com.paper.mes.health.config;

import com.paper.mes.health.service.DataHealthService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataHealthSchedulerTest {

    @Test
    void inspect_runsHealthScan() {
        DataHealthService service = mock(DataHealthService.class);

        new DataHealthScheduler(service).inspect();

        verify(service).inspect();
    }

    @Test
    void inspect_whenScanFails_keepsSchedulerAlive() {
        DataHealthService service = mock(DataHealthService.class);
        doThrow(new IllegalStateException("database unavailable")).when(service).inspect();

        assertDoesNotThrow(() -> new DataHealthScheduler(service).inspect());
    }
}

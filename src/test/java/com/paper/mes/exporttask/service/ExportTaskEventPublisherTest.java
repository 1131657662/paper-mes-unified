package com.paper.mes.exporttask.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskEventPublisherTest {
    @Test
    void subscribe_registersConnectionAndAcceptsTaskEvents() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ExportTaskEventPublisher publisher = new ExportTaskEventPublisher(meterRegistry);

        publisher.subscribe("user-1");
        publisher.publish("user-1", "task-1", 3);
        publisher.heartbeat();

        assertThat(meterRegistry.get("paper_mes_export_sse_connections").gauge().value())
                .isEqualTo(1);
    }
}

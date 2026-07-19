package com.paper.mes.exporttask.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExportTaskEventSynchronizerTest {
    @Test
    void synchronize_publishesInitialAndChangedRevisionToLocalSubscribers() {
        ExportTaskEventPublisher publisher = mock(ExportTaskEventPublisher.class);
        ExportTaskEventRevisionReader reader = mock(ExportTaskEventRevisionReader.class);
        when(publisher.subscriberUserUuids()).thenReturn(Set.of("user-1"));
        when(reader.read(Set.of("user-1"))).thenReturn(Map.of("user-1", "1:1"), Map.of("user-1", "1:2"));
        ExportTaskEventSynchronizer synchronizer = new ExportTaskEventSynchronizer(
                publisher, reader, new SimpleMeterRegistry());

        synchronizer.synchronize();
        synchronizer.synchronize();

        verify(publisher, times(2)).publishRefresh("user-1");
    }

    @Test
    void synchronize_whenNoSubscribers_skipsDatabaseRead() {
        ExportTaskEventPublisher publisher = mock(ExportTaskEventPublisher.class);
        ExportTaskEventRevisionReader reader = mock(ExportTaskEventRevisionReader.class);
        when(publisher.subscriberUserUuids()).thenReturn(Set.of());
        ExportTaskEventSynchronizer synchronizer = new ExportTaskEventSynchronizer(
                publisher, reader, new SimpleMeterRegistry());

        synchronizer.synchronize();

        verifyNoInteractions(reader);
    }
}

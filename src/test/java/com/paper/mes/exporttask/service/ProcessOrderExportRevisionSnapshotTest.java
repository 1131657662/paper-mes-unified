package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import com.paper.mes.processorder.service.FinishCustomerRevisionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessOrderExportRevisionSnapshotTest {
    private FinishCustomerRevisionReader revisionReader;
    private ProcessOrderExportRevisionSnapshot snapshot;

    @BeforeEach
    void setUp() {
        revisionReader = mock(FinishCustomerRevisionReader.class);
        snapshot = new ProcessOrderExportRevisionSnapshot(revisionReader, new ObjectMapper());
    }

    @Test
    void capture_whenExpectedRevisionMatches_persistsCurrentRevision() {
        when(revisionReader.latestRevision("order-1")).thenReturn(revision(3));

        String payload = snapshot.capture("order-1", 3);

        assertThat(payload).contains("\"schemaVersion\":1", "\"customerRevisionNo\":3");
    }

    @Test
    void capture_whenExpectedRevisionIsStale_rejectsTaskCreation() {
        when(revisionReader.latestRevision("order-1")).thenReturn(revision(4));

        assertThatThrownBy(() -> snapshot.capture("order-1", 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本已变化");
    }

    @Test
    void verifyCurrent_whenRevisionIsUnchanged_allowsExport() {
        when(revisionReader.latestRevision("order-1")).thenReturn(revision(2));

        assertThatCode(() -> snapshot.verifyCurrent(
                "order-1", "{\"schemaVersion\":1,\"customerRevisionNo\":2}"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyCurrent_whenRevisionChanged_rejectsStaleExport() {
        when(revisionReader.latestRevision("order-1")).thenReturn(revision(3));

        assertThatThrownBy(() -> snapshot.verifyCurrent(
                "order-1", "{\"schemaVersion\":1,\"customerRevisionNo\":2}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新创建导出任务");
    }

    private FinishCustomerRevision revision(int revisionNo) {
        FinishCustomerRevision revision = new FinishCustomerRevision();
        revision.setRevisionNo(revisionNo);
        return revision;
    }
}

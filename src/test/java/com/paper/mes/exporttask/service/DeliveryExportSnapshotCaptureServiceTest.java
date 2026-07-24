package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.exporttask.entity.ExportTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryExportSnapshotCaptureServiceTest {

    @Test
    void captureInventory_createsHeaderPersistsRowsAndFinalizesCount() {
        DeliveryInventoryMapper inventoryMapper = mock(DeliveryInventoryMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DeliveryInventoryFinishVO row = new DeliveryInventoryFinishVO();
        row.setFinishRollNo("C001");
        when(inventoryMapper.finishRows(any(), anyLong(), anyLong())).thenReturn(List.of(row));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1});
        DeliveryExportSnapshotCaptureService service = new DeliveryExportSnapshotCaptureService(
                inventoryMapper, mock(DeliveryOrderMapper.class), jdbcTemplate,
                new ExportTaskPayloadWriter(new ObjectMapper().findAndRegisterModules()));
        ExportTask task = new ExportTask();
        task.setUuid("task-1");
        task.setQuerySnapshotUuid("snapshot-1");

        service.captureInventory(task, new DeliveryInventoryFinishQuery());

        verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
        ArgumentCaptor<BatchPreparedStatementSetter> setter =
                ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), setter.capture());
        assertThat(setter.getValue().getBatchSize()).isEqualTo(1);
    }
}

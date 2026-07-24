package com.paper.mes.exporttask.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.service.DeliveryExportService;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.exporttask.entity.ExportTask;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryOrderDetailExportTaskHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void generate_whenOrderRemainsCurrent_verifiesFingerprintAfterWriting() throws Exception {
        Fixture fixture = fixture(detail(1), detail(1));
        DeliveryCustomerRevisionPreviewVO preview = new DeliveryCustomerRevisionPreviewVO();
        when(fixture.snapshot.verifyCurrentAndRead("delivery-1", "payload")).thenReturn(preview);

        fixture.handler.generate(task(), tempDir.resolve("delivery.xlsx"));

        verify(fixture.exportService).buildWorkbook(fixture.firstDetail, preview);
        verify(fixture.snapshot).verifyCurrent("delivery-1", "payload");
    }

    @Test
    void generate_whenCurrentOrderBecomesVoided_rejectsNonVoidSnapshotAfterWriting() throws Exception {
        Fixture fixture = fixture(detail(1), detail(3));
        DeliveryCustomerRevisionPreviewVO preview = new DeliveryCustomerRevisionPreviewVO();
        when(fixture.snapshot.verifyCurrentAndRead("delivery-1", "payload")).thenReturn(preview);
        doThrow(new BusinessException("出库单状态已变化，请重新创建导出任务"))
                .when(fixture.snapshot).verifyVoided("payload");

        assertThatThrownBy(() -> fixture.handler.generate(task(), tempDir.resolve("delivery.xlsx")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态已变化");

        verify(fixture.snapshot).verifyVoided("payload");
        verify(fixture.snapshot, never()).verifyCurrent("delivery-1", "payload");
    }

    @Test
    void generate_whenVoidedOrderRemainsVoided_verifiesVoidSnapshotTwice() throws Exception {
        Fixture fixture = fixture(detail(3), detail(3));

        fixture.handler.generate(task(), tempDir.resolve("delivery.xlsx"));

        verify(fixture.snapshot, org.mockito.Mockito.times(2)).verifyVoided("payload");
        verify(fixture.exportService).buildWorkbook(fixture.firstDetail, null);
    }

    private Fixture fixture(DeliveryDetailVO firstDetail, DeliveryDetailVO currentDetail) {
        DeliveryService deliveryService = mock(DeliveryService.class);
        DeliveryExportService exportService = mock(DeliveryExportService.class);
        DeliveryOrderExportRevisionSnapshot snapshot = mock(DeliveryOrderExportRevisionSnapshot.class);
        when(deliveryService.getDetail("delivery-1")).thenReturn(firstDetail, currentDetail);
        when(exportService.buildWorkbook(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.nullable(DeliveryCustomerRevisionPreviewVO.class)))
                .thenReturn(mock(Workbook.class));
        return new Fixture(new DeliveryOrderDetailExportTaskHandler(
                deliveryService, exportService, snapshot), exportService, snapshot, firstDetail);
    }

    private DeliveryDetailVO detail(int status) {
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryNo("CK-001");
        order.setDeliveryStatus(status);
        DeliveryDetailVO detail = new DeliveryDetailVO();
        detail.setOrder(order);
        return detail;
    }

    private ExportTask task() {
        ExportTask task = new ExportTask();
        task.setSourceUuid("delivery-1");
        task.setRequestPayload("payload");
        return task;
    }

    private record Fixture(DeliveryOrderDetailExportTaskHandler handler,
                           DeliveryExportService exportService,
                           DeliveryOrderExportRevisionSnapshot snapshot,
                           DeliveryDetailVO firstDetail) { }
}

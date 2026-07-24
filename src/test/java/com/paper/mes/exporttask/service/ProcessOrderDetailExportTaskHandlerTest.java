package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.service.ProcessOrderExportService;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderDetailExportTaskHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void generate_verifiesCustomerRevisionBeforeAndAfterWorkbookGeneration() throws Exception {
        ProcessOrderService orderService = mock(ProcessOrderService.class);
        ProcessOrderExportService exportService = mock(ProcessOrderExportService.class);
        ProcessOrderExportRevisionSnapshot revisionSnapshot = mock(ProcessOrderExportRevisionSnapshot.class);
        ProcessOrderDetailExportTaskHandler handler = new ProcessOrderDetailExportTaskHandler(
                orderService, exportService, revisionSnapshot);
        ProcessOrderDetailVO detail = detail();
        when(orderService.getDetail("order-1")).thenReturn(detail);
        when(exportService.buildWorkbook(detail)).thenReturn(mock(Workbook.class));

        handler.generate(task(), tempDir.resolve("order.xlsx"));

        verify(revisionSnapshot, times(2)).verifyCurrent(
                "order-1", "{\"schemaVersion\":1,\"customerRevisionNo\":2}");
    }

    private ProcessOrderDetailVO detail() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-001");
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order);
        return detail;
    }

    private ExportTask task() {
        ExportTask task = new ExportTask();
        task.setSourceUuid("order-1");
        task.setRequestPayload("{\"schemaVersion\":1,\"customerRevisionNo\":2}");
        return task;
    }
}

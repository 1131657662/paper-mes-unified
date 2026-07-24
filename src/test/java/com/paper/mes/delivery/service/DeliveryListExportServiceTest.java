package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.dto.DeliveryListExportRow;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.exporttask.service.DeliveryExportSnapshotReader;
import com.paper.mes.exporttask.service.DeliveryExportSnapshotMetadata;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeliveryListExportServiceTest {

    @Test
    void exportToPath_whenResultExceedsPageLimit_writesEveryMatchingOrder() throws Exception {
        DeliveryOrderMapper mapper = mock(DeliveryOrderMapper.class);
        List<DeliveryOrder> orders = IntStream.rangeClosed(1, 151).mapToObj(this::order).toList();
        when(mapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> page(
                invocation.<Page<DeliveryOrder>>getArgument(0), orders));
        Path target = Files.createTempFile("delivery-list-export", ".xlsx");

        try {
            new DeliveryListExportService(mapper, mock(DeliveryExportSnapshotReader.class))
                    .exportToPath(new DeliveryQuery(), target);

            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(target))) {
                assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(151);
                assertThat(workbook.getSheetAt(0).getRow(151).getCell(0).getStringCellValue())
                        .isEqualTo("CK000151");
            }
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    void exportSnapshotToPath_readsFrozenRowsAndWritesSnapshotMetadata() throws Exception {
        DeliveryOrderMapper mapper = mock(DeliveryOrderMapper.class);
        DeliveryExportSnapshotReader reader = mock(DeliveryExportSnapshotReader.class);
        LocalDateTime capturedAt = LocalDateTime.of(2026, 7, 24, 10, 30);
        when(reader.metadata("snapshot-1")).thenReturn(new DeliveryExportSnapshotMetadata(
                "snapshot-1", DeliveryExportSnapshotMetadata.RECONCILIATION_TYPE, capturedAt, 1));
        DeliveryListExportRow row = DeliveryListExportRow.from(order(1));
        when(reader.rows("snapshot-1", 0, 100, DeliveryListExportRow.class)).thenReturn(List.of(row));
        Path target = Files.createTempFile("delivery-list-snapshot", ".xlsx");

        try {
            new DeliveryListExportService(mapper, reader).exportSnapshotToPath("snapshot-1", target);

            verifyNoInteractions(mapper);
            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(target))) {
                assertThat(workbook.getSheet("出库对账").getRow(1).getCell(0).getStringCellValue())
                        .isEqualTo("CK000001");
                assertThat(workbook.getSheet("导出说明").getRow(0).getCell(1).getStringCellValue())
                        .isEqualTo("snapshot-1");
                assertThat(workbook.getSheet("导出说明").getRow(2).getCell(1).getNumericCellValue())
                        .isEqualTo(1);
            }
        } finally {
            Files.deleteIfExists(target);
        }
    }

    private Page<DeliveryOrder> page(Page<DeliveryOrder> request, List<DeliveryOrder> orders) {
        int start = Math.toIntExact((request.getCurrent() - 1) * request.getSize());
        int end = Math.min(start + Math.toIntExact(request.getSize()), orders.size());
        Page<DeliveryOrder> result = Page.of(request.getCurrent(), request.getSize(), false);
        result.setRecords(start >= orders.size() ? List.of() : orders.subList(start, end));
        return result;
    }

    private DeliveryOrder order(int index) {
        DeliveryOrder order = new DeliveryOrder();
        order.setDeliveryNo("CK%06d".formatted(index));
        order.setDeliveryStatus(1);
        return order;
    }
}

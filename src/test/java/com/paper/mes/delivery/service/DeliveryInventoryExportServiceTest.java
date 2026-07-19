package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryInventoryExportServiceTest {

    @Test
    void exportToPath_writesFilteredInventoryWorkbook() throws Exception {
        DeliveryInventoryMapper mapper = mock(DeliveryInventoryMapper.class);
        when(mapper.finishRows(any(), anyLong(), anyLong()))
                .thenReturn(List.of(finish()), List.of());
        Path target = Files.createTempFile("delivery-inventory-export", ".xlsx");

        try {
            new DeliveryInventoryExportService(mapper)
                    .exportToPath(new DeliveryInventoryFinishQuery(), target);

            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(target))) {
                assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                        .isNotBlank();
                assertThat(workbook.getSheetAt(0).getRow(1).getCell(14).getStringCellValue())
                        .isNotBlank();
            }
        } finally {
            Files.deleteIfExists(target);
        }
    }

    private DeliveryInventoryFinishVO finish() {
        DeliveryInventoryFinishVO item = new DeliveryInventoryFinishVO();
        item.setCustomerName("customer");
        item.setFinishRollNo("A000001");
        item.setRemainingWeight(new BigDecimal("120.5"));
        item.setSourceType(1);
        item.setStockState(1);
        return item;
    }
}

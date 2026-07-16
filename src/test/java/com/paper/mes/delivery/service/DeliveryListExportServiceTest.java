package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryListExportServiceTest {

    @Test
    void export_whenResultExceedsPageLimit_writesEveryMatchingOrder() throws Exception {
        DeliveryOrderMapper mapper = mock(DeliveryOrderMapper.class);
        List<DeliveryOrder> orders = IntStream.rangeClosed(1, 151).mapToObj(this::order).toList();
        when(mapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> page(
                invocation.<Page<DeliveryOrder>>getArgument(0), orders));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DeliveryListExportService(mapper).export(new DeliveryQuery(), response);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(151);
            assertThat(workbook.getSheetAt(0).getRow(151).getCell(0).getStringCellValue()).isEqualTo("CK000151");
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

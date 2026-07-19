package com.paper.mes.delivery.service.impl;

import com.paper.mes.delivery.dto.DeliveryInventoryCustomerQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerVO;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityRequest;
import com.paper.mes.delivery.dto.DeliveryInventoryFilter;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventorySummaryVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryInventoryServiceImplTest {

    @Test
    void summaryReturnsMapperAggregation() {
        DeliveryInventoryMapper mapper = mock(DeliveryInventoryMapper.class);
        DeliveryInventorySummaryVO summary = new DeliveryInventorySummaryVO();
        DeliveryInventoryFilter filter = new DeliveryInventoryFilter();
        when(mapper.summary(filter)).thenReturn(summary);

        var result = service(mapper).summary(filter);

        assertThat(result).isSameAs(summary);
    }

    @Test
    void pageCustomers_boundsPageSizeAndReturnsMapperRows() {
        DeliveryInventoryMapper mapper = mock(DeliveryInventoryMapper.class);
        DeliveryInventoryCustomerVO row = new DeliveryInventoryCustomerVO();
        when(mapper.customerCount(any())).thenReturn(1L);
        when(mapper.customerRows(any(), anyLong(), anyLong())).thenReturn(List.of(row));
        DeliveryInventoryCustomerQuery query = new DeliveryInventoryCustomerQuery();
        query.setCurrent(0);
        query.setSize(500);

        var result = service(mapper).pageCustomers(query);

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.getRecords()).containsExactly(row);
        verify(mapper).customerRows(query, 0, 100);
    }

    @Test
    void pageFinishesUsesRequestedSecondPageOffset() {
        DeliveryInventoryMapper mapper = mock(DeliveryInventoryMapper.class);
        when(mapper.finishCount(any())).thenReturn(21L);
        when(mapper.finishRows(any(), anyLong(), anyLong())).thenReturn(List.of(new DeliveryInventoryFinishVO()));
        DeliveryInventoryFinishQuery query = new DeliveryInventoryFinishQuery();
        query.setCustomerUuid("customer-1");
        query.setCurrent(2);
        query.setSize(20);

        var result = service(mapper).pageFinishes(query);

        assertThat(result.getTotal()).isEqualTo(21);
        verify(mapper).finishRows(query, 20, 20);
    }

    @Test
    void validateAvailability_removesLockedAndMissingFinishes() {
        DeliveryInventoryMapper mapper = mock(DeliveryInventoryMapper.class);
        DeliveryInventoryFinishVO available = finish("finish-1", 1);
        DeliveryInventoryFinishVO locked = finish("finish-2", 2);
        when(mapper.availabilityRows(any(), any(), any())).thenReturn(List.of(available, locked));
        DeliveryInventoryAvailabilityRequest request = new DeliveryInventoryAvailabilityRequest();
        request.setCustomerUuid("customer-1");
        request.setWarehouseUuid("warehouse-1");
        request.setFinishUuids(List.of("finish-1", "finish-2", "finish-3"));

        var result = service(mapper).validateAvailability(request);

        assertThat(result.getAvailableFinishUuids()).containsExactly("finish-1");
        assertThat(result.getUnavailable()).extracting("finishUuid")
                .containsExactly("finish-2", "finish-3");
    }

    private DeliveryInventoryServiceImpl service(DeliveryInventoryMapper mapper) {
        return new DeliveryInventoryServiceImpl(mapper);
    }

    private DeliveryInventoryFinishVO finish(String uuid, int stockState) {
        DeliveryInventoryFinishVO finish = new DeliveryInventoryFinishVO();
        finish.setFinishUuid(uuid);
        finish.setStockState(stockState);
        return finish;
    }
}

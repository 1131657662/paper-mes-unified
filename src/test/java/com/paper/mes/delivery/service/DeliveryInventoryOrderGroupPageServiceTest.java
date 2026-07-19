package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventoryOrderGroupVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryOrderGroupMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryInventoryOrderGroupPageServiceTest {

    @Test
    void pagePaginatesOrdersAndBatchAttachesTheirFinishes() {
        DeliveryInventoryOrderGroupMapper mapper = mock(DeliveryInventoryOrderGroupMapper.class);
        DeliveryInventoryFinishQuery query = new DeliveryInventoryFinishQuery();
        query.setCurrent(2);
        query.setSize(10);
        DeliveryInventoryOrderGroupVO group = group("order-1");
        DeliveryInventoryFinishVO finish = finish("order-1", "finish-1");
        when(mapper.count(query)).thenReturn(12L);
        when(mapper.rows(query, 10, 10)).thenReturn(List.of(group));
        when(mapper.finishRows(query, List.of("order-1"))).thenReturn(List.of(finish));

        var result = new DeliveryInventoryOrderGroupPageService(mapper).page(query);

        assertThat(result.getTotal()).isEqualTo(12);
        assertThat(result.getRecords()).containsExactly(group);
        assertThat(group.getFinishes()).containsExactly(finish);
        verify(mapper).finishRows(query, List.of("order-1"));
    }

    private DeliveryInventoryOrderGroupVO group(String orderUuid) {
        DeliveryInventoryOrderGroupVO group = new DeliveryInventoryOrderGroupVO();
        group.setOrderUuid(orderUuid);
        return group;
    }

    private DeliveryInventoryFinishVO finish(String orderUuid, String finishUuid) {
        DeliveryInventoryFinishVO finish = new DeliveryInventoryFinishVO();
        finish.setOrderUuid(orderUuid);
        finish.setFinishUuid(finishUuid);
        return finish;
    }
}

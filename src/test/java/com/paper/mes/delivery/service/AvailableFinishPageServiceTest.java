package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.AvailableFinishQuery;
import com.paper.mes.delivery.dto.AvailableFinishStatsVO;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.mapper.AvailableFinishMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvailableFinishPageServiceTest {

    @Test
    void page_boundsRequestAndEnrichesCurrentPageRows() {
        AvailableFinishMapper mapper = mock(AvailableFinishMapper.class);
        AvailableFinishSourceLoader sourceLoader = mock(AvailableFinishSourceLoader.class);
        DeliveryCashSettlementGuard cashGuard = mock(DeliveryCashSettlementGuard.class);
        AvailableFinishVO row = row();
        when(mapper.count(any())).thenReturn(121L);
        when(mapper.rows(any(), anyLong(), anyLong())).thenReturn(List.of(row));
        AvailableFinishStatsVO stats = new AvailableFinishStatsVO();
        stats.setProductCount(121);
        stats.setUnassignedWarehouseCount(18);
        when(mapper.stats(any())).thenReturn(stats);
        when(sourceLoader.load(any())).thenReturn(Map.of("finish-1", List.of(source())));
        when(cashGuard.unsettledCashOrderUuids(any())).thenReturn(Set.of("order-1"));
        AvailableFinishQuery query = new AvailableFinishQuery();
        query.setCustomerUuid("customer-1");
        query.setCurrent(2);
        query.setSize(500);

        var result = new AvailableFinishPageService(mapper, sourceLoader, cashGuard).page(query);

        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.getRecords().getFirst().getSourceMotherRolls()).hasSize(1);
        assertThat(result.getRecords().getFirst().getSettlementRisk()).isTrue();
        assertThat(result.getScopeCounts().product()).isEqualTo(121);
        assertThat(result.getExcluded().unassignedWarehouseCount()).isEqualTo(18);
        assertThat(result.getAsOf()).isNotNull();
        verify(mapper).rows(query, 100, 100);
    }

    private AvailableFinishVO row() {
        AvailableFinishVO row = new AvailableFinishVO();
        row.setFinishUuid("finish-1");
        row.setOrderUuid("order-1");
        row.setSettleType(1);
        return row;
    }

    private AvailableFinishVO.SourceMotherRollVO source() {
        AvailableFinishVO.SourceMotherRollVO source = new AvailableFinishVO.SourceMotherRollVO();
        source.setOriginalUuid("original-1");
        return source;
    }
}

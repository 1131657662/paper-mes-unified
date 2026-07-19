package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paper.mes.settle.dto.SettleListSummaryVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettleListSummaryServiceTest {

    @Test
    void summarize_whenVoidedRowsExist_excludesTheirAmountsFromActiveTotals() {
        SettleOrderMapper mapper = mock(SettleOrderMapper.class);
        when(mapper.selectMaps(any())).thenReturn(List.of(
                row(1, 2, "1000", "0", "1000", "0"),
                row(2, 1, "500", "200", "300", "0"),
                row(4, 1, "900", "0", "900", "0")));

        SettleListSummaryVO result = new SettleListSummaryService(mapper).summarize(new SettleQuery());

        assertThat(result.totalDocumentCount()).isEqualTo(4);
        assertThat(result.voidDocumentCount()).isEqualTo(1);
        assertThat(result.activeTotalAmount()).isEqualByComparingTo("1500");
        assertThat(result.activeUnreceivedAmount()).isEqualByComparingTo("1300");
    }

    @Test
    void summarize_whenStatusIsFiltered_usesTheSameStatusScopeAsTheList() {
        SettleOrderMapper mapper = mock(SettleOrderMapper.class);
        when(mapper.selectMaps(any())).thenReturn(List.of());
        SettleQuery query = new SettleQuery();
        query.setSettleStatus(2);

        new SettleListSummaryService(mapper).summarize(query);

        ArgumentCaptor<QueryWrapper<SettleOrder>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectMaps(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("settle_status");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValue(2);
    }

    private Map<String, Object> row(int status, long count, String total, String received,
                                    String unreceived, String discount) {
        return Map.of("status", status, "document_count", count,
                "total_amount", new BigDecimal(total), "received_amount", new BigDecimal(received),
                "unreceived_amount", new BigDecimal(unreceived), "discount_amount", new BigDecimal(discount));
    }
}

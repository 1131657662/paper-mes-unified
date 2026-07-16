package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryListSummaryVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryListSummaryServiceTest {

    @Test
    void summarize_whenVoidedRowsExist_excludesTheirWeightFromActiveTotals() {
        DeliveryOrderMapper mapper = mock(DeliveryOrderMapper.class);
        when(mapper.selectMaps(any())).thenReturn(List.of(
                row(1, 2, 3, "1200"), row(2, 4, 8, "3400"), row(3, 1, 2, "900")));

        DeliveryListSummaryVO result = new DeliveryListSummaryService(mapper).summarize(new DeliveryQuery());

        assertThat(result.totalDocumentCount()).isEqualTo(7);
        assertThat(result.voidDocumentCount()).isEqualTo(1);
        assertThat(result.activeRollCount()).isEqualTo(11);
        assertThat(result.activeWeight()).isEqualByComparingTo("4600");
    }

    private Map<String, Object> row(int status, long documents, long rolls, String weight) {
        return Map.of("status", status, "document_count", documents, "roll_count", rolls,
                "weight", new BigDecimal(weight));
    }
}

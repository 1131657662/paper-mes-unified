package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryDetailSortPolicyTest {
    @Test
    void sort_keepsRequestedPriorityAndStableOriginalOrder() {
        DeliveryDetailItemVO first = item("a", "A", 10);
        DeliveryDetailItemVO second = item("b", "A", 10);
        DeliveryDetailItemVO third = item("c", "A", 5);

        List<DeliveryDetailItemVO> result = DeliveryDetailSortPolicy.sort(
                List.of(first, second, third),
                List.of(new DeliverySortSpec("paperName", "asc"), new DeliverySortSpec("gramWeight", "desc")));

        assertThat(result).containsExactly(first, second, third);
    }

    @Test
    void sort_putsEmptyValuesLastForBothDirections() {
        DeliveryDetailItemVO empty = item("empty", null, 1);
        DeliveryDetailItemVO value = item("value", "A", 1);

        assertThat(uuids(DeliveryDetailSortPolicy.sort(List.of(empty, value), List.of(new DeliverySortSpec("paperName", "asc")))))
                .containsExactly("value", "empty");
        assertThat(uuids(DeliveryDetailSortPolicy.sort(List.of(empty, value), List.of(new DeliverySortSpec("paperName", "desc")))))
                .containsExactly("value", "empty");
    }

    @Test
    void normalize_rejectsUnknownFields() {
        assertThatThrownBy(() -> DeliveryDetailSortPolicy.normalize(
                List.of(new DeliverySortSpec("rowNumber", "asc"))))
                .isInstanceOf(RuntimeException.class);
    }

    private List<String> uuids(List<DeliveryDetailItemVO> items) {
        return items.stream().map(DeliveryDetailItemVO::getUuid).toList();
    }

    private DeliveryDetailItemVO item(String uuid, String paperName, Integer gramWeight) {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setUuid(uuid);
        item.setPaperName(paperName);
        item.setGramWeight(gramWeight);
        item.setOutWeight(java.math.BigDecimal.ONE);
        return item;
    }
}

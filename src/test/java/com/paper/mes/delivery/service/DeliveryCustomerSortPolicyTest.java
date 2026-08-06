package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryCustomerSortPolicyTest {
    @Test
    void sort_keepsCustomerPrimaryAndSourceSecondaryOrder() {
        DeliveryCustomerSpecVO first = spec("detail-a", "A");
        DeliveryCustomerSpecVO second = spec("detail-b", "A");
        DeliveryDetailItemVO firstDetail = detail("detail-a", "M-10");
        DeliveryDetailItemVO secondDetail = detail("detail-b", "M-2");

        List<DeliveryCustomerSpecVO> result = DeliveryCustomerSortPolicy.sort(
                List.of(first, second), List.of(firstDetail, secondDetail), List.of(
                        new DeliverySortSpec("customerPaperName", "asc"),
                        new DeliverySortSpec("sourceMotherRoll", "asc")));

        assertThat(result).extracting(DeliveryCustomerSpecVO::getDeliveryDetailUuid)
                .containsExactly("detail-b", "detail-a");
    }

    private DeliveryCustomerSpecVO spec(String uuid, String paperName) {
        DeliveryCustomerSpecVO result = new DeliveryCustomerSpecVO();
        result.setDeliveryDetailUuid(uuid);
        result.setCustomerPaperName(paperName);
        return result;
    }

    private DeliveryDetailItemVO detail(String uuid, String source) {
        DeliveryDetailItemVO result = new DeliveryDetailItemVO();
        result.setUuid(uuid);
        result.setOriginalSummary(source);
        return result;
    }
}

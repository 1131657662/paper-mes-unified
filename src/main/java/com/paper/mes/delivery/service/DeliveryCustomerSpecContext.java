package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.entity.DeliveryCustomerRevisionItem;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.processorder.entity.FinishRoll;

public record DeliveryCustomerSpecContext(
        DeliveryDetailItemVO physical,
        DeliveryDetail detail,
        FinishRoll finish,
        DeliveryCustomerRevisionItem previousRevision,
        boolean usePhysicalBaseline) {
}

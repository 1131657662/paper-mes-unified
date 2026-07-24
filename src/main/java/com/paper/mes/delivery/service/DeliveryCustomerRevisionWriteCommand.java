package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionRequestDTO;

public record DeliveryCustomerRevisionWriteCommand(
        String deliveryUuid,
        String requestHash,
        DeliveryCustomerRevisionPreviewVO preview,
        DeliveryCustomerRevisionRequestDTO request) {
}

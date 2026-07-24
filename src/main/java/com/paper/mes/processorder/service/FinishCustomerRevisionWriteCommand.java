package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;

public record FinishCustomerRevisionWriteCommand(
        String orderUuid,
        String requestHash,
        FinishCustomerRevisionPreviewVO preview,
        FinishCustomerRevisionRequestDTO request) {
}

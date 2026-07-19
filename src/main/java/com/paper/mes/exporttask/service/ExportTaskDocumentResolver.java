package com.paper.mes.exporttask.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.service.SettleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportTaskDocumentResolver {
    private final SettleService settleService;
    private final ProcessOrderService processOrderService;
    private final DeliveryService deliveryService;

    public SettleOrder settle(String uuid) {
        return requireDocument(settleService.getDetailOrder(uuid), "结算单不存在");
    }

    public ProcessOrder processOrder(String uuid) {
        return requireDocument(processOrderService.getById(uuid), "加工单不存在");
    }

    public DeliveryOrder deliveryOrder(String uuid) {
        return requireDocument(deliveryService.getById(uuid), "出库单不存在");
    }

    private <T> T requireDocument(T document, String message) {
        if (document == null) throw new BusinessException(ResultCode.NOT_FOUND, message);
        return document;
    }
}

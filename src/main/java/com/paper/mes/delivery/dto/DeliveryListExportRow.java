package com.paper.mes.delivery.dto;

import com.paper.mes.delivery.entity.DeliveryOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DeliveryListExportRow(
        String deliveryNo, String customerName, LocalDate deliveryDate, Integer totalCount,
        BigDecimal totalWeight, String pickerName, String carNo, String containerNo,
        Integer deliveryStatus, Integer settleBlockAction, String signUser,
        LocalDateTime signTime, String remark, LocalDateTime createTime) {

    public static DeliveryListExportRow from(DeliveryOrder order) {
        return new DeliveryListExportRow(
                order.getDeliveryNo(), order.getCustomerName(), order.getDeliveryDate(), order.getTotalCount(),
                order.getTotalWeight(), order.getPickerName(), order.getCarNo(), order.getContainerNo(),
                order.getDeliveryStatus(), order.getSettleBlockAction(), order.getSignUser(),
                order.getSignTime(), order.getRemark(), order.getCreateTime());
    }
}

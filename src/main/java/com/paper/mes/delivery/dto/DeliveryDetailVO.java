package com.paper.mes.delivery.dto;

import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import lombok.Data;

import java.util.List;

/**
 * 出库单详情（主表 + 明细列表）。
 */
@Data
public class DeliveryDetailVO {

    private DeliveryOrder order;
    private List<DeliveryDetail> details;
}

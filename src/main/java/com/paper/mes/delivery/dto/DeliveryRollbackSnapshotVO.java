package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库回退快照：用于在回退后的待出库单中追溯上一版签收单据。
 */
@Data
public class DeliveryRollbackSnapshotVO {

    private String deliveryNo;
    private String rollbackReason;
    private String rollbackOperator;
    private LocalDateTime rollbackTime;
    private String signUser;
    private LocalDateTime signTime;
    private Integer totalCount;
    private BigDecimal totalWeight;
    private List<DeliveryDetailItemVO> details;
}

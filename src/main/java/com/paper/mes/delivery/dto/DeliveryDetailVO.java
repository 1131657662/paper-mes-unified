package com.paper.mes.delivery.dto;

import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.oplog.entity.OperationLog;
import lombok.Data;

import java.util.List;

/**
 * 出库单详情（主表 + 明细列表）。
 */
@Data
public class DeliveryDetailVO {

    private DeliveryOrder order;
    private List<DeliveryDetailItemVO> details;
    private List<OperationLog> operationLogs;
    private DeliveryRollbackSnapshotVO rollbackSnapshot;
}

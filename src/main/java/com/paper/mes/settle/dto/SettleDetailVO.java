package com.paper.mes.settle.dto;

import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import lombok.Data;

import java.util.List;

/**
 * 结算单详情（主表 + 加工单明细列表 + 收款流水列表）。
 */
@Data
public class SettleDetailVO {

    private SettleOrder order;
    private List<SettleDetail> details;
    private List<ReceiveRecord> receives;
    private List<SettlePrintLineVO> printLines;
    private List<OperationLog> operationLogs;
}

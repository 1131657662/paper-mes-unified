package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_remain_inventory_ledger")
public class RemainInventoryLedger {

    @TableId
    private String uuid;
    private String lotUuid;
    private String registrationLineUuid;
    private String sourceFinishRollUuid;
    private String eventType;
    private BigDecimal weightDelta;
    private BigDecimal beforeWeight;
    private BigDecimal afterWeight;
    private String requestId;
    private String reason;
    private String reversalOfUuid;
    private String createBy;
    private LocalDateTime createTime;
}

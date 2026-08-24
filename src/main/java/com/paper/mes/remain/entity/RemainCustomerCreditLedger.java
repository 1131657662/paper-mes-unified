package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_remain_customer_credit_ledger")
public class RemainCustomerCreditLedger {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String accountUuid;
    private String adjustmentUuid;
    private String customerUuid;
    private String eventType;
    private BigDecimal amount;
    private BigDecimal weight;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private String requestId;
    private String reversalOfUuid;
    private String createBy;
    private LocalDateTime createTime;
}

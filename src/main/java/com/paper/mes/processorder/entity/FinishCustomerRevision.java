package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_finish_customer_revision")
public class FinishCustomerRevision extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String orderUuid;
    private Integer revisionNo;
    private String requestId;
    private String requestHash;
    private String sourceStage;
    private String reason;
    private Integer itemCount;
    private BigDecimal customerTotalWeight;
}

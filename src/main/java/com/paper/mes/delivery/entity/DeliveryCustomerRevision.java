package com.paper.mes.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_delivery_customer_revision")
public class DeliveryCustomerRevision extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String deliveryUuid;
    private Integer revisionNo;
    private String requestId;
    private String requestHash;
    private String reason;
    private Integer itemCount;
    private BigDecimal customerTotalWeight;
}

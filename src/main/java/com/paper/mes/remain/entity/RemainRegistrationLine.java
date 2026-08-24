package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_remain_registration_line")
public class RemainRegistrationLine extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String registrationUuid;
    private String sourceFinishRollUuid;
    private String sourceOrderUuid;
    private String sourceCustomerUuid;
    private BigDecimal sourceSystemWeight;
    private BigDecimal transferredSystemWeight;
    private BigDecimal rolledBackSystemWeight;
    private BigDecimal processedSystemWeight;
    private BigDecimal currentOwnWeight;
    private BigDecimal amount;
    private BigDecimal appliedAmount;
    private BigDecimal appliedWeight;
    private String status;
}

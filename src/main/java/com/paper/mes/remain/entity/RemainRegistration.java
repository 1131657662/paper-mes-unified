package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_remain_registration")
public class RemainRegistration extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String registrationNo;
    private String requestId;
    private String requestHash;
    private String orderUuid;
    private String customerUuid;
    private LocalDateTime registrationDate;
    private String confirmationName;
    private String confirmationChannel;
    private LocalDateTime confirmationAt;
    private String confirmationEvidence;
    private String status;
    private String priceStatus;
    private Integer priceVersion;
    private String pricingBasis;
    private LocalDateTime priceConfirmedAt;
    private String priceConfirmedBy;
    private BigDecimal totalTransferredWeight;
    private BigDecimal totalRolledBackWeight;
    private BigDecimal totalProcessedWeight;
    private BigDecimal totalAmount;
    private String remark;
}

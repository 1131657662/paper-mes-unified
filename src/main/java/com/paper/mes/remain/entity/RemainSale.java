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
@TableName("biz_remain_sale")
public class RemainSale extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String saleNo;
    private String requestId;
    private String requestHash;
    private String saleKind;
    private String reversalOfUuid;
    private LocalDateTime processDate;
    private String warehouseUuid;
    private String pricingMode;
    private BigDecimal systemWeight;
    private BigDecimal actualWeight;
    private BigDecimal unitPrice;
    private BigDecimal calculatedAmount;
    private BigDecimal receivedAmount;
    private String buyerName;
    private String vehicleNo;
    private String weighingTicketNo;
    private String weighingEvidence;
    private String status;
    private String reason;
}

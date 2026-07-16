package com.paper.mes.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出库单主表 biz_delivery_order。与加工单解耦，已完成加工单的成品即可勾选出库。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_delivery_order")
public class DeliveryOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String deliveryNo;
    private String customerUuid;
    private String customerName;
    private LocalDate deliveryDate;
    private Integer totalCount;
    private BigDecimal totalWeight;
    private String pickerName;
    private String carNo;
    private String containerNo;
    private String signUser;
    private LocalDateTime signTime;
    /** 现结拦截结果 0无 1警告放行 2拦截 */
    private Integer settleBlockAction;
    /** 1待出库 2已出库签收 3已作废 */
    private Integer deliveryStatus;
    private String voidReason;
    private String voidBy;
    private LocalDateTime voidTime;
    private String snapDelivery;
    private LocalDateTime snapDeliveryTime;
    private String remark;
}

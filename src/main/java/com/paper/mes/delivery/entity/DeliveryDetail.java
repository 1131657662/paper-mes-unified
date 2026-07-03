package com.paper.mes.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 出库成品明细表 biz_delivery_detail。统一关联成品 biz_finish_roll（含 source_type=2 直发记录）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_delivery_detail")
public class DeliveryDetail extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String deliveryUuid;
    private String finishUuid;
    private String orderUuid;
    private String finishRollNo;
    private String paperName;
    private BigDecimal outWeight;
    /** 1占用待出库库存 0历史明细不占用 */
    private Integer stockLockStatus;
    private String remark;
}

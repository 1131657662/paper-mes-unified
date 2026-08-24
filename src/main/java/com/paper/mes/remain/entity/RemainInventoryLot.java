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
@TableName("biz_remain_inventory_lot")
public class RemainInventoryLot extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String registrationLineUuid;
    private String sourceFinishRollUuid;
    private String customerUuid;
    private String warehouseUuid;
    private BigDecimal currentWeight;
    private String status;
    private String priceStatus;
}

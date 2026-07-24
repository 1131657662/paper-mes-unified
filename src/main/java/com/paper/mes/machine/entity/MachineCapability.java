package com.paper.mes.machine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_machine_process_capability")
public class MachineCapability extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String machineUuid;
    private String catalogUuid;
    private Integer isDefault;
    private Integer priority;
    private Integer minWidth;
    private Integer maxWidth;
    private BigDecimal maxRollWeight;
    private Integer maxDiameter;
    private String remark;
}

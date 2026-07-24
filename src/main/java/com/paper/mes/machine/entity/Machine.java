package com.paper.mes.machine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机台档案 sys_machine。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_machine")
public class Machine extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String machineCode;
    private String machineName;
    /** 机台类型 1锯纸 2复卷 3通用 */
    private Integer machineType;
    /** MACHINE设备 WORKSTATION工位 */
    private String resourceKind;
    /** 1启用 2停用 */
    private Integer status;
    private String remark;
}

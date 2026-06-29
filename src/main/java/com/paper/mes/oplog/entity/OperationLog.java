package com.paper.mes.oplog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志表 sys_operation_log。
 * 记录字段修改/打印/回录/结算/作废卷号/超差放行等动作留痕。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_operation_log")
public class OperationLog extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    /** 业务类型（加工单/出库单/结算单等） */
    private String bizType;
    /** 关联业务主键 */
    private String bizUuid;
    /** 冗余业务单号 */
    private String bizNo;
    /** 动作类型（打印/回录/作废卷号/超差放行等） */
    private String actionType;
    /** 字段级日志：变更字段名 */
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String operator;
    private LocalDateTime operateTime;
    /** 备注（如补打原因、超差放行原因） */
    private String remark;
}

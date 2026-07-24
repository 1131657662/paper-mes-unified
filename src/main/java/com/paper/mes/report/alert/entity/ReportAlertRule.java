package com.paper.mes.report.alert.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rpt_alert_rule")
public class ReportAlertRule {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String signalCode;
    private String ruleName;
    private Integer scopeType;
    private String customerUuid;
    private String paperUuid;
    private Integer processType;
    private String comparisonOperator;
    private BigDecimal thresholdValue;
    private Integer severity;
    private Integer isEnabled;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}

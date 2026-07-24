package com.paper.mes.report.subscription.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("rpt_report_subscription")
public class ReportSubscription {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String ownerUuid;
    private String subscriptionName;
    private String reportPath;
    private Integer scheduleType;
    private LocalTime executionTime;
    private Integer weekDay;
    private Integer monthDay;
    private String timezone;
    private String reportQuery;
    private Integer periodPolicy;
    private Integer releasePolicy;
    private String pinnedReleaseUuid;
    private String deliveryChannel;
    private Integer isEnabled;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastScheduledAt;
    private String lastErrorMessage;
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

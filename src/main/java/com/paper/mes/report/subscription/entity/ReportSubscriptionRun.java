package com.paper.mes.report.subscription.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rpt_report_subscription_run")
public class ReportSubscriptionRun {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String subscriptionUuid;
    private LocalDateTime scheduledFor;
    private String metricReleaseUuid;
    private Integer runStatus;
    private Integer plannedCount;
    private Integer dispatchedCount;
    private Integer failedCount;
    private String errorMessage;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

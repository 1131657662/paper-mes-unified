package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Immutable business history for issued print versions. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_order_issue_version")
public class ProcessOrderIssueVersion extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private Integer versionNo;
    private Integer previousVersionNo;
    private String snapshotBefore;
    private String snapshotAfter;
    private String changeReason;
    private String operatorName;
    private LocalDateTime changeTime;
    private LocalDateTime issueTime;
    private String issueOperatorName;
    private String requestId;
    private String payloadHash;
    private String status;
}

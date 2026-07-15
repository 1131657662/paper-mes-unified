package com.paper.mes.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class SystemNotification extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String recipientUuid;
    private String notificationType;
    private String severity;
    private String title;
    private String content;
    private String sourceType;
    private String sourceUuid;
    private LocalDateTime readAt;
}

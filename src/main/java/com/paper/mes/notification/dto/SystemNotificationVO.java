package com.paper.mes.notification.dto;

import com.paper.mes.notification.entity.SystemNotification;

import java.time.LocalDateTime;

public record SystemNotificationVO(
        String uuid,
        String notificationType,
        String severity,
        String title,
        String content,
        String sourceType,
        String sourceUuid,
        boolean read,
        LocalDateTime createdAt) {

    public static SystemNotificationVO from(SystemNotification notification) {
        return new SystemNotificationVO(
                notification.getUuid(),
                notification.getNotificationType(),
                notification.getSeverity(),
                notification.getTitle(),
                notification.getContent(),
                notification.getSourceType(),
                notification.getSourceUuid(),
                notification.getReadAt() != null,
                notification.getCreateTime());
    }
}

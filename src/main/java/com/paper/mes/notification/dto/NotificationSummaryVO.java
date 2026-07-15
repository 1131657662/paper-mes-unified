package com.paper.mes.notification.dto;

import java.util.List;

public record NotificationSummaryVO(long unreadCount, List<SystemNotificationVO> items) {
}

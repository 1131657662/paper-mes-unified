package com.paper.mes.notification.controller;

import com.paper.mes.common.R;
import com.paper.mes.notification.dto.NotificationSummaryVO;
import com.paper.mes.notification.service.SystemNotificationService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class SystemNotificationController {

    private final SystemNotificationService notificationService;

    @GetMapping
    public R<NotificationSummaryVO> summary() {
        return R.success(notificationService.currentUserSummary());
    }

    @PutMapping("/{uuid}/read")
    public R<Void> markRead(@PathVariable @Pattern(regexp = "[0-9A-Za-z-]{1,36}") String uuid) {
        notificationService.markRead(uuid);
        return R.success();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead();
        return R.success();
    }
}

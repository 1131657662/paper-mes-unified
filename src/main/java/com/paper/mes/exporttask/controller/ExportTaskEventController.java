package com.paper.mes.exporttask.controller;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.exporttask.service.ExportTaskEventPublisher;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/export-tasks")
@RequiredArgsConstructor
public class ExportTaskEventController {
    private final ExportTaskEventPublisher publisher;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission(Permissions.EXPORT_TASK_VIEW)
    public SseEmitter events(HttpServletResponse response) {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        return publisher.subscribe(user.getUuid());
    }

    @ExceptionHandler(IOException.class)
    public void handleClientDisconnect() {
        // Closing an SSE tab is a normal client lifecycle event, not an API failure.
    }
}

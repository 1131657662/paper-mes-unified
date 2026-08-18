package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.parse.ProcessAiParseConfirmationService;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.ai.process.stream.ProcessAiParseStreamService;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/process-orders/{orderUuid}/ai/process-parse")
@RequirePermission(Permissions.ORDER_CREATE)
@RequiredArgsConstructor
public class ProcessAiParseController {

    private final ProcessAiParseStreamService streamService;
    private final ProcessAiParseConfirmationService confirmationService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String orderUuid,
            @Valid @RequestBody ProcessAiParseStreamRequest request,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        return streamService.start(orderUuid, request);
    }

    @PostMapping("/confirm")
    public R<ProcessAiConfirmResponse> confirm(
            @PathVariable String orderUuid,
            @Valid @RequestBody ProcessAiConfirmRequest request) {
        return R.success(confirmationService.confirm(orderUuid, request));
    }
}

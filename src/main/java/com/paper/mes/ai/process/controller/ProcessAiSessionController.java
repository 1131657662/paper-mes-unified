package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.dto.ProcessAiSessionRequest;
import com.paper.mes.ai.process.session.dto.ProcessAiSessionResponse;
import com.paper.mes.ai.process.session.dto.ProcessAiMemoryRefreshRequest;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process-orders/{orderUuid}/ai/process-parse")
@RequirePermission(Permissions.ORDER_CREATE)
@RequiredArgsConstructor
public class ProcessAiSessionController {

    private final ProcessAiConversationService conversationService;

    @PostMapping("/session")
    public R<ProcessAiSessionResponse> open(
            @PathVariable String orderUuid,
            @Valid @RequestBody ProcessAiSessionRequest request) {
        return R.success(conversationService.open(orderUuid, request));
    }

    @PostMapping("/session/{conversationId}/refresh-memory")
    public R<ProcessAiSessionResponse> refreshMemory(
            @PathVariable String orderUuid,
            @PathVariable String conversationId,
            @Valid @RequestBody ProcessAiMemoryRefreshRequest request) {
        return R.success(conversationService.refreshMemory(
                orderUuid, conversationId, request.expectedVersion()));
    }
}

package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/process-orders/{orderUuid}/ai/process-parse")
@RequirePermission(Permissions.ORDER_CREATE)
@RequiredArgsConstructor
public class ProcessAiMessageController {

    private final ProcessAiMessageService messageService;

    @GetMapping("/session/{conversationId}/messages")
    public R<List<ProcessAiMessageResponse>> messages(
            @PathVariable String orderUuid,
            @PathVariable String conversationId,
            @RequestParam @Min(0) int expectedVersion) {
        return R.success(messageService.restore(orderUuid, conversationId, expectedVersion));
    }
}

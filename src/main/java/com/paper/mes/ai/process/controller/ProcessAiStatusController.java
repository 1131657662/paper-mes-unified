package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.status.ProcessAiStatusResponse;
import com.paper.mes.ai.process.status.ProcessAiStatusService;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequirePermission(Permissions.AI_ASSIST)
@RequiredArgsConstructor
public class ProcessAiStatusController {

    private final ProcessAiStatusService statusService;

    @GetMapping("/process-status")
    public R<ProcessAiStatusResponse> status() {
        return R.success(statusService.status());
    }
}

package com.paper.mes.ai.controller;

import com.paper.mes.ai.dto.AiAssistRequest;
import com.paper.mes.ai.dto.AiAssistResponse;
import com.paper.mes.ai.dto.AiStatusResponse;
import com.paper.mes.ai.service.AiAssistService;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai")
@RequirePermission(Permissions.AI_ASSIST)
@RequiredArgsConstructor
public class AiAssistController {

    private final AiAssistService assistService;

    @GetMapping("/status")
    public R<AiStatusResponse> status() {
        return R.success(assistService.status());
    }

    @PostMapping("/assist")
    public R<AiAssistResponse> assist(@Valid @RequestBody AiAssistRequest request) {
        return R.success(assistService.assist(request));
    }
}

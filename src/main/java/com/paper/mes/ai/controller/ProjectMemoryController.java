package com.paper.mes.ai.controller;

import com.paper.mes.ai.memory.ProjectMemoryManagementService;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryResponse;
import com.paper.mes.ai.memory.dto.ProjectMemoryRollbackRequest;
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
@RequestMapping("/api/ai/project-memory")
@RequiredArgsConstructor
public class ProjectMemoryController {

    private final ProjectMemoryManagementService service;

    @GetMapping("/current")
    @RequirePermission(Permissions.AI_ASSIST)
    public R<ProjectMemoryResponse> current() {
        return R.success(service.current());
    }

    @PostMapping("/reload")
    @RequirePermission(Permissions.AI_ASSIST)
    public R<ProjectMemoryResponse> reload() {
        return R.success(service.reload());
    }

    @PostMapping("/patch")
    @RequirePermission(Permissions.AI_MEMORY_MANAGE)
    public R<ProjectMemoryResponse> patch(@Valid @RequestBody ProjectMemoryPatchRequest request) {
        return R.success(service.patch(request));
    }

    @PostMapping("/rollback")
    @RequirePermission(Permissions.AI_MEMORY_MANAGE)
    public R<ProjectMemoryResponse> rollback(@Valid @RequestBody ProjectMemoryRollbackRequest request) {
        return R.success(service.rollback(request));
    }
}

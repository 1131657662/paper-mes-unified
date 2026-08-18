package com.paper.mes.ai.controller;

import com.paper.mes.ai.memory.ProjectMemoryManagementService;
import com.paper.mes.ai.memory.ProjectMemoryVersionQueryService;
import com.paper.mes.ai.memory.candidate.ProjectMemoryCandidateManagementService;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateApproveRequest;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateDetailResponse;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateRejectRequest;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateResponse;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryResponse;
import com.paper.mes.ai.memory.dto.ProjectMemoryRollbackRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryVersionResponse;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/ai/project-memory")
@RequiredArgsConstructor
public class ProjectMemoryController {

    private final ProjectMemoryManagementService service;
    private final ProjectMemoryVersionQueryService versionQueryService;
    private final ProjectMemoryCandidateManagementService candidateService;

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

    @GetMapping("/versions")
    @RequirePermission(Permissions.AI_ASSIST)
    public R<List<ProjectMemoryVersionResponse>> versions() {
        return R.success(versionQueryService.versions());
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

    @GetMapping("/candidates")
    @RequirePermission(Permissions.AI_MEMORY_MANAGE)
    public R<List<ProjectMemoryCandidateResponse>> candidates(
            @RequestParam(required = false) String status) {
        return R.success(candidateService.list(status));
    }

    @GetMapping("/candidates/{uuid}")
    @RequirePermission(Permissions.AI_MEMORY_MANAGE)
    public R<ProjectMemoryCandidateDetailResponse> candidate(@PathVariable String uuid) {
        return R.success(candidateService.detail(uuid));
    }

    @PostMapping("/candidates/{uuid}/approve")
    @RequirePermission(Permissions.AI_MEMORY_MANAGE)
    public R<ProjectMemoryResponse> approveCandidate(
            @PathVariable String uuid,
            @Valid @RequestBody ProjectMemoryCandidateApproveRequest request) {
        return R.success(candidateService.approve(uuid, request));
    }

    @PostMapping("/candidates/{uuid}/reject")
    @RequirePermission(Permissions.AI_MEMORY_MANAGE)
    public R<ProjectMemoryCandidateResponse> rejectCandidate(
            @PathVariable String uuid,
            @Valid @RequestBody ProjectMemoryCandidateRejectRequest request) {
        return R.success(candidateService.reject(uuid, request));
    }
}

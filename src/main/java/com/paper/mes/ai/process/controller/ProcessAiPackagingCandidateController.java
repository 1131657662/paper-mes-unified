package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.parse.ProcessAiPackagingCandidateQueryService;
import com.paper.mes.ai.process.parse.ProcessAiPackagingCandidateResolutionService;
import com.paper.mes.ai.process.parse.dto.ProcessAiPendingPackagingCandidate;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/process-orders/{orderUuid}/ai/process-parse/packaging-candidates")
@RequirePermission(Permissions.ORDER_CREATE)
@RequiredArgsConstructor
public class ProcessAiPackagingCandidateController {

    private final ProcessAiPackagingCandidateQueryService queryService;
    private final ProcessAiPackagingCandidateResolutionService resolutionService;

    @GetMapping
    public R<List<ProcessAiPendingPackagingCandidate>> pending(
            @PathVariable String orderUuid,
            @RequestParam @Min(0) int expectedVersion) {
        return R.success(queryService.pending(orderUuid, expectedVersion));
    }

    @PostMapping("/{parseId}/{ownerRollRef}/dismiss")
    public R<Void> dismiss(
            @PathVariable String orderUuid,
            @PathVariable String parseId,
            @PathVariable String ownerRollRef,
            @RequestParam @Min(0) int expectedVersion) {
        resolutionService.dismiss(orderUuid, parseId, ownerRollRef, expectedVersion);
        return R.success();
    }
}

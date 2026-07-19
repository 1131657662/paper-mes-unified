package com.paper.mes.exporttask.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.exporttask.dto.ExportTaskOperationsVO;
import com.paper.mes.exporttask.dto.ExportTaskOperationsIssuesVO;
import com.paper.mes.exporttask.service.ExportTaskOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export-tasks")
@RequiredArgsConstructor
public class ExportTaskOperationsController {
    private final ExportTaskOperationsService operationsService;

    @GetMapping("/operations")
    @RequirePermission(Permissions.SYSTEM_AUDIT)
    public R<ExportTaskOperationsVO> operations() {
        return R.success(operationsService.overview());
    }

    @GetMapping("/operations/issues")
    @RequirePermission(Permissions.SYSTEM_AUDIT)
    public R<ExportTaskOperationsIssuesVO> issues() {
        return R.success(operationsService.issues());
    }
}

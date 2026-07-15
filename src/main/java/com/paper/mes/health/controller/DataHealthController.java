package com.paper.mes.health.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.health.dto.DataHealthSummaryVO;
import com.paper.mes.health.dto.DataHealthRepairRequest;
import com.paper.mes.health.dto.DataHealthRepairResultVO;
import com.paper.mes.health.service.DataHealthRepairService;
import com.paper.mes.health.service.DataHealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/data-health")
@RequirePermission(Permissions.DATA_HEALTH)
@RequiredArgsConstructor
public class DataHealthController {

    private final DataHealthService dataHealthService;
    private final DataHealthRepairService dataHealthRepairService;

    @GetMapping
    public R<DataHealthSummaryVO> inspect() {
        return R.success(dataHealthService.inspect());
    }

    @PostMapping("/settlements/{uuid}/reconcile")
    public R<DataHealthRepairResultVO> reconcileSettlement(
            @PathVariable String uuid, @Valid @RequestBody DataHealthRepairRequest request) {
        return R.success(dataHealthRepairService.reconcileSettlement(uuid, request));
    }

    @PostMapping("/process-orders/{uuid}/restore-completed")
    public R<DataHealthRepairResultVO> restoreCompletedOrder(
            @PathVariable String uuid, @Valid @RequestBody DataHealthRepairRequest request) {
        return R.success(dataHealthRepairService.restoreCompletedOrder(uuid, request));
    }
}

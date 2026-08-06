package com.paper.mes.processorder.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.processorder.dto.ProcessOrderAppendDTO;
import com.paper.mes.processorder.dto.ProcessOrderAppendVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.service.ProcessOrderAppendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process-orders/{orderUuid}/append-sessions")
@RequirePermission(Permissions.ORDER_CREATE)
@RequiredArgsConstructor
public class ProcessOrderAppendController {

    private final ProcessOrderAppendService service;

    @PostMapping
    public R<ProcessOrderAppendVO> start(@PathVariable String orderUuid,
                                         @Valid @RequestBody ProcessOrderAppendDTO.Create request) {
        return R.success(service.start(orderUuid, request));
    }

    @GetMapping("/{sessionUuid}")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<ProcessOrderAppendVO> get(@PathVariable String orderUuid,
                                       @PathVariable String sessionUuid) {
        return R.success(service.get(orderUuid, sessionUuid));
    }

    @PutMapping("/{sessionUuid}/rolls")
    public R<ProcessOrderAppendVO> saveRolls(@PathVariable String orderUuid,
                                             @PathVariable String sessionUuid,
                                             @Valid @RequestBody ProcessOrderAppendDTO.RollBatch request) {
        return R.success(service.saveRolls(orderUuid, sessionUuid, request));
    }

    @PutMapping("/{sessionUuid}/rolls/process-settings")
    public R<ProcessOrderAppendVO> saveProcessSettings(@PathVariable String orderUuid,
                                                       @PathVariable String sessionUuid,
                                                       @Valid @RequestBody ProcessOrderAppendDTO.ProcessSettings request) {
        return R.success(service.saveProcessSettings(orderUuid, sessionUuid, request));
    }

    @PutMapping("/{sessionUuid}/rolls/{rollUuid}/process-plan")
    public R<ProcessOrderAppendVO> savePlan(@PathVariable String orderUuid,
                                            @PathVariable String sessionUuid,
                                            @PathVariable String rollUuid,
                                            @Valid @RequestBody ProcessOrderAppendDTO.PlanSave request) {
        request.setRollUuid(rollUuid);
        return R.success(service.savePlan(orderUuid, sessionUuid, request));
    }

    @PostMapping("/{sessionUuid}/rolls/{rollUuid}/process-plan/preview")
    public R<PlanPreviewVO> previewPlan(@PathVariable String orderUuid,
                                        @PathVariable String sessionUuid,
                                        @PathVariable String rollUuid,
                                        @Valid @RequestBody ProcessOrderAppendDTO.PlanPreview request) {
        return R.success(service.previewPlan(orderUuid, sessionUuid, rollUuid, request));
    }

    @PostMapping("/{sessionUuid}/preview")
    public R<ProcessOrderAppendVO> preview(@PathVariable String orderUuid,
                                           @PathVariable String sessionUuid,
                                           @Valid @RequestBody ProcessOrderAppendDTO.Preview request) {
        return R.success(service.preview(orderUuid, sessionUuid, request));
    }

    @PostMapping("/{sessionUuid}/commit")
    public R<ProcessOrderAppendVO.CommitResult> commit(@PathVariable String orderUuid,
                                                       @PathVariable String sessionUuid,
                                                       @Valid @RequestBody ProcessOrderAppendDTO.Commit request) {
        return R.success(service.commit(orderUuid, sessionUuid, request));
    }

    @DeleteMapping("/{sessionUuid}")
    public R<Void> cancel(@PathVariable String orderUuid, @PathVariable String sessionUuid) {
        service.cancel(orderUuid, sessionUuid);
        return R.success();
    }
}

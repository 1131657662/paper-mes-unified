package com.paper.mes.processorder.controller;

import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import com.paper.mes.processorder.dto.DraftOrderVO;
import com.paper.mes.processorder.dto.DraftProgressDTO;
import com.paper.mes.processorder.dto.DraftSummaryVO;
import com.paper.mes.processorder.dto.OriginalRollBatchSaveDTO;
import com.paper.mes.processorder.dto.OriginalRollImportPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessConfigDraftSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanPreviewRequestDTO;
import com.paper.mes.processorder.dto.ProcessOrderSubmitVO;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/process-orders")
@RequirePermission(Permissions.ORDER_CREATE)
@RequiredArgsConstructor
public class ProcessOrderDraftController {

    private final ProcessOrderDraftService draftService;

    @GetMapping("/drafts")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<List<DraftSummaryVO>> listDrafts() {
        return R.success(draftService.listDrafts());
    }

    @GetMapping("/{orderUuid}/draft")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<DraftOrderVO> getDraft(@PathVariable String orderUuid) {
        return R.success(draftService.getDraft(orderUuid));
    }

    @PostMapping("/drafts")
    public R<String> createDraft(@Valid @RequestBody DraftOrderBaseDTO dto) {
        return R.success(draftService.createDraft(dto));
    }

    @PutMapping("/{orderUuid}/base-info")
    public R<Void> saveBaseInfo(@PathVariable String orderUuid,
                                @Valid @RequestBody DraftOrderBaseDTO dto) {
        draftService.saveBaseInfo(orderUuid, dto);
        return R.success();
    }

    @PutMapping("/{orderUuid}/draft-progress")
    public R<Void> saveDraftProgress(@PathVariable String orderUuid,
                                     @Valid @RequestBody DraftProgressDTO dto) {
        draftService.saveDraftProgress(orderUuid, dto.getCurrentStep());
        return R.success();
    }

    @PutMapping("/{orderUuid}/original-rolls")
    public R<List<String>> replaceOriginalRolls(@PathVariable String orderUuid,
                                                @Valid @RequestBody OriginalRollBatchSaveDTO dto) {
        return R.success(draftService.replaceOriginalRolls(orderUuid, dto.getRolls()));
    }

    @PostMapping("/{orderUuid}/original-rolls/import-preview")
    public R<OriginalRollImportPreviewVO> importPreview(@PathVariable String orderUuid,
                                                       @RequestParam("file") MultipartFile file) {
        return R.success(draftService.importPreview(file));
    }

    @PostMapping("/{orderUuid}/rolls/plan-preview")
    public R<PlanPreviewVO> previewProcessPlan(@PathVariable String orderUuid,
                                               @Valid @RequestBody ProcessPlanPreviewRequestDTO dto) {
        return R.success(draftService.previewProcessPlan(orderUuid, dto.getOriginalUuid(), dto.getPlan()));
    }

    @PutMapping("/{orderUuid}/rolls/{rollUuid}/process-plan")
    public R<PlanPreviewVO> saveProcessPlan(@PathVariable String orderUuid,
                                            @PathVariable String rollUuid,
                                            @Valid @RequestBody ProcessPlanPreviewRequestDTO dto) {
        return R.success(draftService.saveProcessPlan(orderUuid, rollUuid, dto.getPlan()));
    }

    @PutMapping("/{orderUuid}/rolls/process-plan/batch")
    public R<List<PlanPreviewVO>> saveProcessPlanBatch(@PathVariable String orderUuid,
                                                       @Valid @RequestBody ProcessPlanBatchSaveDTO dto) {
        return R.success(draftService.saveProcessPlanBatch(orderUuid, dto));
    }

    @PutMapping("/{orderUuid}/rolls/{rollUuid}/process-config")
    public R<Void> saveProcessConfig(@PathVariable String orderUuid,
                                     @PathVariable String rollUuid,
                                     @Valid @RequestBody ProcessConfigDraftSaveDTO dto) {
        draftService.saveProcessConfig(orderUuid, rollUuid, dto.getConfig());
        return R.success();
    }

    @PostMapping("/{orderUuid}/submit")
    public R<ProcessOrderSubmitVO> submit(@PathVariable String orderUuid) {
        return R.success(draftService.submit(orderUuid));
    }
}

package com.paper.mes.processorder.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveVO;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveVO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.OriginalRollRemarkDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PhysicalReprintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderPrintViewVO;
import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.dto.ProcessOrderRemarkDTO;
import com.paper.mes.processorder.dto.ProcessOrderRollbackDTO;
import com.paper.mes.processorder.dto.ProcessOrderVoidDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchResultVO;
import com.paper.mes.processorder.dto.ProcessStepPricingAdjustmentDTO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.SnapshotDiffVO;
import com.paper.mes.processorder.dto.StatusChangeDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteAppendService;
import com.paper.mes.processorder.service.ProcessRouteSaveService;
import com.paper.mes.processorder.service.ProcessStepPricingBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequiredArgsConstructor
public class ProcessOrderController {

    private final ProcessOrderService processOrderService;
    private final ProcessRouteSaveService processRouteSaveService;
    private final ProcessRouteAppendService processRouteAppendService;
    private final ProcessStepPricingBatchService processStepPricingBatchService;

    @GetMapping
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<PageResult<ProcessOrder>> page(ProcessOrderQuery query) {
        return R.success(processOrderService.pageOrders(query));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<ProcessOrderDetailVO> detail(@PathVariable String uuid) {
        return R.success(processOrderService.getDetail(uuid));
    }

    @GetMapping("/{uuid}/print-view")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<ProcessOrderPrintViewVO> printView(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "ISSUED") PrintViewVersion version) {
        return R.success(processOrderService.getPrintView(uuid, version));
    }

    @PostMapping
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<String> create(@Valid @RequestBody ProcessOrderCreateDTO dto) {
        return R.success(processOrderService.create(dto));
    }

    @PostMapping("/{orderUuid}/rolls")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<String> addRoll(@PathVariable String orderUuid,
                             @Valid @RequestBody OriginalRollDTO dto) {
        return R.success(processOrderService.addRoll(orderUuid, dto));
    }

    @PutMapping("/{uuid}/remarks")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<Void> updateOrderRemark(@PathVariable String uuid,
                                     @Valid @RequestBody ProcessOrderRemarkDTO dto) {
        processOrderService.updateOrderRemark(uuid, dto);
        return R.success();
    }

    @PutMapping("/rolls/{rollUuid}")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<Void> updateRoll(@PathVariable String rollUuid,
                              @Valid @RequestBody OriginalRollDTO dto) {
        processOrderService.updateRoll(rollUuid, dto);
        return R.success();
    }

    @PutMapping("/rolls/{rollUuid}/remarks")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<Void> updateRollRemark(@PathVariable String rollUuid,
                                    @Valid @RequestBody OriginalRollRemarkDTO dto) {
        processOrderService.updateRollRemark(rollUuid, dto);
        return R.success();
    }

    @DeleteMapping("/rolls/{rollUuid}")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<Void> deleteRoll(@PathVariable String rollUuid) {
        processOrderService.deleteRoll(rollUuid);
        return R.success();
    }

    @PostMapping("/{orderUuid}/rolls/{rollUuid}/finish-config")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<FinishConfigSaveVO> saveFinishConfig(@PathVariable String orderUuid,
                                                  @PathVariable String rollUuid,
                                                  @Valid @RequestBody FinishConfigSaveDTO dto) {
        return R.success(processOrderService.saveFinishConfig(orderUuid, rollUuid, dto));
    }

    @PostMapping("/{orderUuid}/finish-config/batch")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<FinishConfigBatchSaveVO> saveFinishConfigBatch(
            @PathVariable String orderUuid,
            @Valid @RequestBody FinishConfigBatchSaveDTO dto) {
        return R.success(processOrderService.saveFinishConfigBatch(orderUuid, dto));
    }

    @PostMapping("/{orderUuid}/rolls/{rollUuid}/rewind-plan/preview")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<FinishPreviewVO> previewRewindPlan(@PathVariable String orderUuid,
                                                @PathVariable String rollUuid,
                                                @Valid @RequestBody RewindPlanPreviewDTO dto) {
        return R.success(processOrderService.previewRewindPlan(orderUuid, rollUuid, dto));
    }

    @PutMapping("/{uuid}/status")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> changeStatus(@PathVariable String uuid,
                                @Valid @RequestBody StatusChangeDTO dto) {
        processOrderService.changeStatus(uuid, dto.getTargetStatus(), dto.getReason());
        return R.success();
    }

    @PutMapping("/{uuid}/to-record")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> completeProcessing(@PathVariable String uuid,
                                      @RequestBody(required = false) StatusChangeDTO dto) {
        processOrderService.completeProcessing(uuid, dto == null ? null : dto.getReason());
        return R.success();
    }

    @PutMapping("/{uuid}/rollback-draft")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> rollbackToDraft(@PathVariable String uuid,
                                   @Valid @RequestBody ProcessOrderRollbackDTO dto) {
        processOrderService.rollbackToDraft(uuid, dto.getReason());
        return R.success();
    }

    @PutMapping("/{uuid}/void")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> voidOrder(@PathVariable String uuid,
                             @Valid @RequestBody ProcessOrderVoidDTO dto) {
        processOrderService.voidOrder(uuid, dto);
        return R.success();
    }

    @PutMapping("/rolls/{rollUuid}/status")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> changeRollStatus(@PathVariable String rollUuid,
                                    @Valid @RequestBody StatusChangeDTO dto) {
        processOrderService.changeRollStatus(rollUuid, dto.getTargetStatus());
        return R.success();
    }

    @PostMapping("/{uuid}/print")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<PrintResultVO> print(@PathVariable String uuid,
                                  @RequestBody(required = false) PrintDTO dto) {
        return R.success(processOrderService.print(uuid, dto == null ? new PrintDTO() : dto));
    }

    @PostMapping("/{uuid}/physical-reprint")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<PrintResultVO> physicalReprint(@PathVariable String uuid,
                                            @Valid @RequestBody PhysicalReprintDTO dto) {
        return R.success(processOrderService.physicalReprint(uuid, dto));
    }

    @PostMapping("/{uuid}/issue")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<PrintResultVO> issue(@PathVariable String uuid) {
        return R.success(processOrderService.issue(uuid));
    }

    @PostMapping("/{uuid}/back-record")
    @RequirePermission(Permissions.ORDER_BACK_RECORD)
    public R<BackRecordResultVO> backRecord(@PathVariable String uuid,
                                            @Valid @RequestBody BackRecordDTO dto) {
        return R.success(processOrderService.backRecord(uuid, dto));
    }

    @PostMapping("/{uuid}/calc-fee")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<FeeResultVO> calcFee(@PathVariable String uuid) {
        return R.success(processOrderService.calcFee(uuid));
    }

    @PutMapping("/steps/{stepUuid}/pricing")
    @RequirePermission(Permissions.ORDER_PRICING)
    public R<FeeResultVO> adjustProcessStepPricing(
            @PathVariable String stepUuid,
            @Valid @RequestBody ProcessStepPricingAdjustmentDTO dto) {
        return R.success(processOrderService.adjustProcessStepPricing(stepUuid, dto));
    }

    @PostMapping("/{orderUuid}/pricing-adjustments/preview")
    @RequirePermission(Permissions.ORDER_PRICING)
    public R<ProcessStepPricingBatchPreviewVO> previewPricingAdjustments(
            @PathVariable String orderUuid,
            @Valid @RequestBody ProcessStepPricingBatchDTO dto) {
        return R.success(processStepPricingBatchService.preview(orderUuid, dto));
    }

    @PutMapping("/{orderUuid}/pricing-adjustments")
    @RequirePermission(Permissions.ORDER_PRICING)
    public R<ProcessStepPricingBatchPreviewVO> applyPricingAdjustments(
            @PathVariable String orderUuid,
            @Valid @RequestBody ProcessStepPricingBatchDTO dto) {
        return R.success(processStepPricingBatchService.apply(orderUuid, dto));
    }

    /** 双版本快照对比差异（P2-6）：下发标称值 vs 完成实际值。 */
    @GetMapping("/{uuid}/snapshot-diff")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<SnapshotDiffVO> snapshotDiff(@PathVariable String uuid) {
        return R.success(processOrderService.snapshotDiff(uuid));
    }

    /** 待下发单据后续工艺预览：用于已有加工单在首道产物基础上继续加工。 */
    @PostMapping("/{orderUuid}/route-preview")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<ProcessRoutePreviewVO> previewProcessRouteForPending(@PathVariable String orderUuid,
                                                                  @Valid @RequestBody ProcessRoutePreviewDTO dto) {
        return R.success(processRouteSaveService.preview(orderUuid, dto));
    }

    /** 待下发单据后续工艺保存：替换该母卷未下发方案，重建阶段产出与最终成品号。 */
    @PostMapping("/{orderUuid}/route-config")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<ProcessRoutePreviewVO> saveProcessRoute(@PathVariable String orderUuid,
                                                     @Valid @RequestBody ProcessRoutePreviewDTO dto) {
        return R.success(processRouteSaveService.save(orderUuid, dto));
    }

    /** 追加后续链式工艺预览：只从已有阶段产物继续加工，不重建旧路线。 */
    @PostMapping("/{orderUuid}/route-append-preview")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<ProcessRoutePreviewVO> previewAppendProcessRoute(@PathVariable String orderUuid,
                                                              @Valid @RequestBody ProcessRoutePreviewDTO dto) {
        return R.success(processRouteAppendService.preview(orderUuid, dto));
    }

    /** 保存追加后续链式工艺：新增工序、阶段产物和最终成品号，不清理已有工艺。 */
    @PostMapping("/{orderUuid}/route-append")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<ProcessRoutePreviewVO> saveAppendProcessRoute(@PathVariable String orderUuid,
                                                           @Valid @RequestBody ProcessRoutePreviewDTO dto) {
        return R.success(processRouteAppendService.save(orderUuid, dto));
    }

    /** 破损图片多图上传并绑定原纸（P2-4）：返回合并后的完整图片 URL 列表。 */
    @PostMapping("/rolls/{rollUuid}/damage-images")
    @RequirePermission(Permissions.ORDER_CREATE)
    public R<List<String>> uploadDamageImages(@PathVariable String rollUuid,
                                              @RequestParam("files") MultipartFile[] files) {
        return R.success(processOrderService.uploadDamageImages(rollUuid, files));
    }

    /** 新增工序（Phase 5.1）：待下发可维护；待回录仅允许新增追加工序。 */
    @PostMapping("/{orderUuid}/steps")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> addProcessStep(@PathVariable String orderUuid,
                                  @Valid @RequestBody ProcessStepDTO dto) {
        processOrderService.addProcessStep(orderUuid, dto);
        return R.success();
    }

    /** 新建加工单工作台批量新增或覆盖附加工艺，整批成功或整批回滚。 */
    @PostMapping("/{orderUuid}/steps/batch")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<ProcessStepBatchResultVO> addProcessSteps(@PathVariable String orderUuid,
                                                       @Valid @RequestBody ProcessStepBatchDTO dto) {
        return R.success(processOrderService.addProcessSteps(orderUuid, dto));
    }

    /** 修改工序：草稿或待下发状态可操作。 */
    @PutMapping("/steps/{stepUuid}")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> updateProcessStep(@PathVariable String stepUuid,
                                     @Valid @RequestBody ProcessStepDTO dto) {
        processOrderService.updateProcessStep(stepUuid, dto);
        return R.success();
    }

    /** 删除工序：草稿或待下发状态可操作，主工艺不可删除。 */
    @DeleteMapping("/steps/{stepUuid}")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<Void> deleteProcessStep(@PathVariable String stepUuid) {
        processOrderService.deleteProcessStep(stepUuid);
        return R.success();
    }
}

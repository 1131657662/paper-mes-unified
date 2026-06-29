package com.paper.mes.processorder.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveVO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.SnapshotDiffVO;
import com.paper.mes.processorder.dto.StatusChangeDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.service.ProcessOrderService;
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

    @GetMapping
    public R<PageResult<ProcessOrder>> page(ProcessOrderQuery query) {
        return R.success(processOrderService.pageOrders(query));
    }

    @GetMapping("/{uuid}")
    public R<ProcessOrderDetailVO> detail(@PathVariable String uuid) {
        return R.success(processOrderService.getDetail(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody ProcessOrderCreateDTO dto) {
        return R.success(processOrderService.create(dto));
    }

    @PostMapping("/{orderUuid}/rolls")
    public R<String> addRoll(@PathVariable String orderUuid,
                             @Valid @RequestBody OriginalRollDTO dto) {
        return R.success(processOrderService.addRoll(orderUuid, dto));
    }

    @PutMapping("/rolls/{rollUuid}")
    public R<Void> updateRoll(@PathVariable String rollUuid,
                              @Valid @RequestBody OriginalRollDTO dto) {
        processOrderService.updateRoll(rollUuid, dto);
        return R.success();
    }

    @DeleteMapping("/rolls/{rollUuid}")
    public R<Void> deleteRoll(@PathVariable String rollUuid) {
        processOrderService.deleteRoll(rollUuid);
        return R.success();
    }

    @PostMapping("/{orderUuid}/rolls/{rollUuid}/finish-config")
    public R<FinishConfigSaveVO> saveFinishConfig(@PathVariable String orderUuid,
                                                  @PathVariable String rollUuid,
                                                  @Valid @RequestBody FinishConfigSaveDTO dto) {
        return R.success(processOrderService.saveFinishConfig(orderUuid, rollUuid, dto));
    }

    @PostMapping("/{orderUuid}/rolls/{rollUuid}/rewind-plan/preview")
    public R<FinishPreviewVO> previewRewindPlan(@PathVariable String orderUuid,
                                                @PathVariable String rollUuid,
                                                @Valid @RequestBody RewindPlanPreviewDTO dto) {
        return R.success(processOrderService.previewRewindPlan(orderUuid, rollUuid, dto));
    }

    @PutMapping("/{uuid}/status")
    public R<Void> changeStatus(@PathVariable String uuid,
                                @Valid @RequestBody StatusChangeDTO dto) {
        processOrderService.changeStatus(uuid, dto.getTargetStatus());
        return R.success();
    }

    @PutMapping("/rolls/{rollUuid}/status")
    public R<Void> changeRollStatus(@PathVariable String rollUuid,
                                    @Valid @RequestBody StatusChangeDTO dto) {
        processOrderService.changeRollStatus(rollUuid, dto.getTargetStatus());
        return R.success();
    }

    @PostMapping("/{uuid}/print")
    public R<PrintResultVO> print(@PathVariable String uuid,
                                  @RequestBody(required = false) PrintDTO dto) {
        return R.success(processOrderService.print(uuid, dto == null ? new PrintDTO() : dto));
    }

    @PostMapping("/{uuid}/back-record")
    public R<BackRecordResultVO> backRecord(@PathVariable String uuid,
                                            @Valid @RequestBody BackRecordDTO dto) {
        return R.success(processOrderService.backRecord(uuid, dto));
    }

    @PostMapping("/{uuid}/calc-fee")
    public R<FeeResultVO> calcFee(@PathVariable String uuid) {
        return R.success(processOrderService.calcFee(uuid));
    }

    /** 双版本快照对比差异（P2-6）：下发标称值 vs 完成实际值。 */
    @GetMapping("/{uuid}/snapshot-diff")
    public R<SnapshotDiffVO> snapshotDiff(@PathVariable String uuid) {
        return R.success(processOrderService.snapshotDiff(uuid));
    }

    /** 破损图片多图上传并绑定原纸（P2-4）：返回合并后的完整图片 URL 列表。 */
    @PostMapping("/rolls/{rollUuid}/damage-images")
    public R<List<String>> uploadDamageImages(@PathVariable String rollUuid,
                                              @RequestParam("files") MultipartFile[] files) {
        return R.success(processOrderService.uploadDamageImages(rollUuid, files));
    }

    /** 新增工序（Phase 5.1）：待下发可维护；待回录仅允许新增追加工序。 */
    @PostMapping("/{orderUuid}/steps")
    public R<Void> addProcessStep(@PathVariable String orderUuid,
                                  @Valid @RequestBody ProcessStepDTO dto) {
        processOrderService.addProcessStep(orderUuid, dto);
        return R.success();
    }

    /** 修改工序（Phase 5.1）：仅待下发状态可操作。 */
    @PutMapping("/steps/{stepUuid}")
    public R<Void> updateProcessStep(@PathVariable String stepUuid,
                                     @Valid @RequestBody ProcessStepDTO dto) {
        processOrderService.updateProcessStep(stepUuid, dto);
        return R.success();
    }

    /** 删除工序（Phase 5.1）：仅待下发状态可操作，主工艺不可删除。 */
    @DeleteMapping("/steps/{stepUuid}")
    public R<Void> deleteProcessStep(@PathVariable String stepUuid) {
        processOrderService.deleteProcessStep(stepUuid);
        return R.success();
    }
}

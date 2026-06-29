package com.paper.mes.processorder.controller;

import com.paper.mes.common.R;
import com.paper.mes.processorder.dto.FinishRollBatchDTO;
import com.paper.mes.processorder.dto.SpareRollAppendDTO;
import com.paper.mes.processorder.dto.SpareRollBatchVoidDTO;
import com.paper.mes.processorder.dto.StatusChangeDTO;
import com.paper.mes.processorder.service.FinishRollService;
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

import java.util.List;

@RestController
@RequestMapping("/api/finish-rolls")
@RequiredArgsConstructor
public class FinishRollController {

    private final FinishRollService finishRollService;

    @PutMapping("/{uuid}/status")
    public R<Void> changeStatus(@PathVariable String uuid,
                                @Valid @RequestBody StatusChangeDTO dto) {
        finishRollService.changeFinishStatus(uuid, dto.getTargetStatus());
        return R.success();
    }

    /** 批量生成正式成品卷号。 */
    @PostMapping("/orders/{orderUuid}/batch")
    public R<List<String>> batchGenerate(@PathVariable String orderUuid,
                                         @Valid @RequestBody FinishRollBatchDTO dto) {
        return R.success(finishRollService.batchGenerate(orderUuid, dto));
    }

    /** 追加备用卷号。 */
    @PostMapping("/orders/{orderUuid}/spare")
    public R<List<String>> appendSpare(@PathVariable String orderUuid,
                                       @Valid @RequestBody SpareRollAppendDTO dto) {
        return R.success(finishRollService.appendSpare(orderUuid, dto));
    }

    /** 作废封存卷号。 */
    @DeleteMapping("/{uuid}/roll-no")
    public R<Void> voidRollNo(@PathVariable String uuid) {
        finishRollService.voidRollNo(uuid);
        return R.success();
    }

    /** 多选一键批量作废备用卷号（文档 §784）。 */
    @PostMapping("/batch-void")
    public R<Integer> batchVoidRollNo(@Valid @RequestBody SpareRollBatchVoidDTO dto) {
        return R.success(finishRollService.batchVoidRollNo(dto.getUuids()));
    }

    /** 卷号全局查重：返回是否可用。 */
    @GetMapping("/check")
    public R<Boolean> checkRollNo(@RequestParam String rollNo,
                                  @RequestParam(required = false) String excludeUuid) {
        return R.success(finishRollService.isRollNoAvailable(rollNo, excludeUuid));
    }
}

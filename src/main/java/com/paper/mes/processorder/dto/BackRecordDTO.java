package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 加工单回录入参（V4.1 §5.7 / P1-4）。
 *
 * 回录提交时：写入原纸复称实际参数、成品实际重量，逐卷做三级闭合校验，
 * 直发卷自动产出沿用母卷号的直发成品，作废未使用备用号，生成完成快照，
 * 状态 待回录(3) → 已完成(4)。
 */
@Data
public class BackRecordDTO {

    /** 回录操作人（无鉴权体系，前端传入；空则落 system）。 */
    private String operator;

    /**
     * >5% 超差放行授权。任一卷闭合校验命中 BLOCK 时：
     *  - authorized=true 且 releaseReason 非空 → 放行并写 sys_operation_log(超差放行)；
     *  - 否则拦截回录。
     */
    private boolean overToleranceAuthorized;
    private String releaseReason;

    @NotEmpty(message = "原纸回录明细不能为空")
    @Valid
    private List<BackRecordRollDTO> rolls;

    @Valid
    private List<BackRecordFinishDTO> finishes;
}

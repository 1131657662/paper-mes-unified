package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

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

    @Size(max = 50, message = "管理员账号长度不能超过50")
    private String releaseAdminUsername;

    @ToString.Exclude
    @Size(max = 128, message = "管理员密码长度不能超过128")
    private String releaseAdminPassword;

    @Size(max = 500, message = "放行原因长度不能超过500")
    private String releaseReason;

    @Size(max = 500, message = "偏差原因长度不能超过500")
    private String varianceReason;

    @NotEmpty(message = "原纸回录明细不能为空")
    @Size(max = 500, message = "单次原纸回录不能超过500条")
    @Valid
    private List<BackRecordRollDTO> rolls;

    @Valid
    @Size(max = 500, message = "单次成品回录不能超过500条")
    private List<BackRecordFinishDTO> finishes;

    /** 现场定尺回录时实际保留并进入余料库存的切边。 */
    @Valid
    @Size(max = 500, message = "单次余料回录不能超过500条")
    private List<BackRecordTrimDTO> trims;

    @Valid
    @Size(max = 100, message = "单次工序回录不能超过100条")
    private List<BackRecordStepDTO> steps;
}

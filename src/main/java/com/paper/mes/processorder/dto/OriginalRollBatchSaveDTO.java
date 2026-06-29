package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 草稿阶段批量替换原纸明细。
 */
@Data
public class OriginalRollBatchSaveDTO {

    @NotEmpty(message = "原纸明细不能为空")
    @Valid
    private List<OriginalRollDTO> rolls;
}

package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 草稿阶段批量替换原纸明细。
 */
@Data
public class OriginalRollBatchSaveDTO {

    @jakarta.validation.constraints.NotNull(message = "草稿版本不能为空")
    @jakarta.validation.constraints.Min(value = 0, message = "草稿版本不能小于0")
    private Integer expectedVersion;

    @NotEmpty(message = "原纸明细不能为空")
    @Size(max = 500, message = "单次原纸明细不能超过500条")
    @Valid
    private List<OriginalRollDTO> rolls;
}

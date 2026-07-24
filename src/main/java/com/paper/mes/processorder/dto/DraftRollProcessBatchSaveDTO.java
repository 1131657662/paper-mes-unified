package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 草稿加工方式步骤的批量保存入参。 */
@Data
public class DraftRollProcessBatchSaveDTO {

    @NotNull(message = "草稿版本不能为空")
    @Min(value = 0, message = "草稿版本不能小于0")
    private Integer expectedVersion;

    @Valid
    @NotEmpty(message = "母卷工艺选择不能为空")
    @Size(max = 500, message = "单次母卷工艺选择不能超过500条")
    private List<DraftRollProcessDTO> rolls;
}

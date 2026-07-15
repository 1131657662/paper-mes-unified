package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 追加备用卷号入参（is_spare=1，顺延全局流水）。
 */
@Data
public class SpareRollAppendDTO {

    @NotNull(message = "追加数量不能为空")
    @Min(value = 1, message = "追加数量至少为 1")
    @Max(value = 500, message = "单次追加数量不超过 500")
    private Integer count;

    /** 来源母卷；多母卷加工单必填，单母卷可由后端自动解析。 */
    private String originalUuid;
}

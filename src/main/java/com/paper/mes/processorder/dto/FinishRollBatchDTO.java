package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量生成成品卷号入参。
 * 生成 count 个正式成品卷号（is_spare=0），并以下列字段作为成品行模板。
 */
@Data
public class FinishRollBatchDTO {

    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为 1")
    @Max(value = 500, message = "单次生成数量不超过 500")
    private Integer count;

    /** 来源母卷；多母卷加工单必填，单母卷可由后端自动解析。 */
    private String originalUuid;

    /** 成品品名（模板，可空，回录时再补全）。 */
    private String paperName;
    private String customerPaperName;
    private Integer gramWeight;
    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    private String warehouseUuid;
    private String remark;
}

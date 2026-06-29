package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 多选批量作废备用卷号入参（文档 V4.1 §784 batch-void）。
 * 整批校验：任一卷号非「未使用的预生成号」即整体回滚。
 */
@Data
public class SpareRollBatchVoidDTO {

    @NotEmpty(message = "作废卷号清单不能为空")
    @Size(max = 500, message = "单次批量作废不超过 500 条")
    private List<@NotBlank(message = "卷号uuid不能为空") String> uuids;
}

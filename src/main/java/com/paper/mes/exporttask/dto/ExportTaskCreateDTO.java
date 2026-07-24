package com.paper.mes.exporttask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExportTaskCreateDTO {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestId;

    /** 加工单或出库单导出时固定的客户口径版本；其他单据类型为空。 */
    @Min(value = 0, message = "客户口径版本不能小于0")
    private Integer customerRevisionNo;
}

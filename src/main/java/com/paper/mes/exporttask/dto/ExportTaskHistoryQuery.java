package com.paper.mes.exporttask.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExportTaskHistoryQuery {
    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 1000000, message = "页码超出允许范围")
    private Integer current = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    @Min(value = 1, message = "任务状态无效")
    @Max(value = 6, message = "任务状态无效")
    private Integer taskStatus;

    @Size(max = 32, message = "业务模块标识不能超过32个字符")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "业务模块标识格式无效")
    private String moduleCode;

    @Size(max = 80, message = "搜索关键词不能超过80个字符")
    private String keyword;

    private Boolean attentionOnly = false;
}

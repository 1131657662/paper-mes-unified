package com.paper.mes.exporttask.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportTaskAcknowledgeDTO {
    @NotNull(message = "数据截点不能为空")
    @PastOrPresent(message = "数据截点不能晚于当前时间")
    private LocalDateTime asOf;

    @Min(value = 1, message = "任务状态无效")
    @Max(value = 6, message = "任务状态无效")
    private Integer taskStatus;

    @Size(max = 32, message = "业务模块标识不能超过32个字符")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "业务模块标识格式无效")
    private String moduleCode;
    @Size(max = 50, message = "操作标识不能超过50个字符")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "操作标识格式无效")
    private String operationCode;

    @Size(max = 80, message = "搜索关键词不能超过80个字符")
    private String keyword;
}

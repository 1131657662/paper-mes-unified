package com.paper.mes.exporttask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExportTaskCreateDTO {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestId;
}

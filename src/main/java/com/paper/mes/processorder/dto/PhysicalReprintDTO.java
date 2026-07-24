package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhysicalReprintDTO {

    @NotNull(message = "打印版本不能为空")
    private PrintViewVersion version;

    @NotBlank(message = "补打原因不能为空")
    @Size(max = 255, message = "补打原因不能超过255个字符")
    private String reason;
}

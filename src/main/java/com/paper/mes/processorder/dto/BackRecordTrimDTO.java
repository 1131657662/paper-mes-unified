package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BackRecordTrimDTO {

    @NotBlank(message = "切边来源母卷不能为空")
    private String originalUuid;

    @NotNull(message = "切边宽度不能为空")
    @Positive(message = "切边宽度必须大于0")
    private Integer finishWidth;

    @NotNull(message = "切边实际重量不能为空")
    @DecimalMin(value = "0.001", message = "切边实际重量必须大于0")
    private BigDecimal actualWeight;

    @Size(max = 255, message = "切边备注不能超过255个字符")
    private String actualRemark;
}
